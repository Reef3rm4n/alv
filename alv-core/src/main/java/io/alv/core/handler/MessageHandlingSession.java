package io.alv.core.handler;

import io.alv.core.ErrorType;
import io.alv.core.MessageHandler;
import io.alv.core.cluster.storage.Lmdb;
import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.input.InputMessage;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import io.alv.core.handler.messages.objects.Error;
import io.alv.core.handler.messages.objects.MessageEnvelope;
import io.alv.core.handler.messages.output.Ack;
import io.alv.core.handler.messages.output.ErrorMessage;
import io.alv.core.handler.messages.output.Event;
import org.agrona.DirectBuffer;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MessageHandlingSession<M> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlingSession.class);
  private final Lmdb lmdb;
  private final MessageHandler<M> messageHandler;
  private final ClientSessionManager clientSessionManager;
  private final ScheduledMessagesHandler scheduledMessagesHandler;

  public MessageHandlingSession(
    final ScheduledMessagesHandler scheduledMessagesHandler,
    final ClientSessionManager clientSessionManager,
    final MessageHandler<M> messageHandler,
    final Lmdb lmdb
  ) {
    this.scheduledMessagesHandler = scheduledMessagesHandler;
    this.clientSessionManager = clientSessionManager;
    this.messageHandler = messageHandler;
    this.lmdb = lmdb;
  }

  public void onMessage(InputMessage inputMessage, long timestamp, long clientSessionId, long logPosition) {
    LOGGER.debug("Handling message handler={} logPosition={} snowflake={} sessionId={} timestamp={}", messageHandler.getClass().getSimpleName(), logPosition, inputMessage.snowflake(), clientSessionId, timestamp);
    deserializeMessage(inputMessage, timestamp, clientSessionId).ifPresent(
      message -> {
        try (var txn = lmdb.txnWrite()) {
          validate(message, timestamp, txn)
            .ifPresentOrElse(
              error -> sendError(error, inputMessage, timestamp, clientSessionId),
              () -> handleMessage(inputMessage, timestamp, clientSessionId, logPosition, message, txn)
            );
        }
      }
    );

  }

  private void handleMessage(InputMessage inputMessage, long timestamp, long clientSessionId, long logPosition, M message, Txn<DirectBuffer> txn) {
    try {
      final var session = new Context<>(message, timestamp, new ReadWriteState(txn, lmdb));
      messageHandler.onMessage(session);
      if (!session.schedule.isEmpty()) {
        session.schedule.forEach(
          (deadline, cmd) -> scheduledMessagesHandler
            .schedule(inputMessage.snowflake(), deadline, cmd)
        );
      }
      if (!session.unicast.isEmpty()) {
        session.unicast.forEach(reply -> reply(reply, inputMessage, timestamp, clientSessionId));
      }
      if (!session.broadcast.isEmpty()) {
        session.broadcast.forEach(event -> broadcast(event, inputMessage, timestamp, clientSessionId));
      }
      clientSessionManager.unicast(clientSessionId, new Ack(inputMessage.snowflake(), timestamp));
      txn.commit();
    } catch (Exception e) {
      LOGGER.error("Handling message handler={} logPosition={} snowflake={} sessionId={} timestamp={}", messageHandler.getClass().getSimpleName(), logPosition, inputMessage.snowflake(), clientSessionId, timestamp, e);
      clientSessionManager.unicast(
        clientSessionId,
        new ErrorMessage(
          timestamp,
          inputMessage.snowflake(),
          new Error(
            ErrorType.HANDLER_EXCEPTION,
            e.getMessage(),
            0
          )
        )
      );
      txn.abort();
    }
  }

  private void sendError(Error error, InputMessage inputMessage, long timestamp, long clientSessionId) {
    clientSessionManager.unicast(clientSessionId, new ErrorMessage(timestamp, inputMessage.snowflake(), error));
  }

  private Optional<Error> validate(M decodedCommand, long timestamp, Txn<DirectBuffer> txn) {
    final var violations = new ArrayList<ConstraintViolation>(3);
    final var session = new ValidationContext<>(decodedCommand, timestamp, violations, new ReadOnlyState(txn, lmdb));
    try {
      messageHandler.onValidation(session);
      if (!violations.isEmpty()) {
        txn.abort();
        return Optional.of(new Error(
            ErrorType.CONSTRAINT_VIOLATION,
            violations.stream()
              .map(ConstraintViolation::message)
              .collect(Collectors.joining(",", "[", "]")),
            violations.stream().map(ConstraintViolation::code).findFirst().orElseThrow()
          )
        );
      } else {
        return Optional.empty();
      }
    } catch (Exception e) {
      txn.abort();
      return Optional.of(new Error(
          ErrorType.VALIDATION_EXCEPTION,
          e.getMessage()
        )
      );
    }
  }

  private void broadcast(Object event, InputMessage inputMessage, long timestamp, long clientSessionId) {
    serializePayload(event, inputMessage, timestamp, clientSessionId)
      .ifPresent(
        payload -> clientSessionManager.broadcast(new Event(
            timestamp,
            0,
            payload
          )
        )
      );
  }

  private void reply(Object response, InputMessage envelope, long timestamp, long clientSessionId) {
    serializePayload(response, envelope, timestamp, clientSessionId)
      .ifPresent(payload -> clientSessionManager.unicast(clientSessionId, new Event(timestamp, envelope.snowflake(), payload)));
  }

  private Optional<MessageEnvelope> serializePayload(Object response, InputMessage inputMessage, long timestamp, long clientSessionId) {
    try {
      return Optional.of(MessageEnvelopeCodec.serialize(response));
    } catch (Exception exception) {
      sendError(new Error(
        ErrorType.SERIALIZATION_EXCEPTION,
        "Error serializing event %s emitted during handling of message %s error=%s".formatted(response.getClass().getSimpleName(), inputMessage.messageEnvelope().payloadType(), exception.getMessage()),
        0
      ), inputMessage, timestamp, clientSessionId);
      return Optional.empty();
    }
  }

  private Optional<M> deserializeMessage(InputMessage inputMessage, long timestamp, long clientSessionId) {
    try {
      return Optional.of(MessageEnvelopeCodec.deserialize(inputMessage.messageEnvelope()));
    } catch (Exception e) {
      LOGGER.error("Error deserializing message", e);
      sendError(new Error(
        ErrorType.DESERIALIZATION_EXCEPTION,
        "Unable to decode message %s error=%s".formatted(inputMessage.messageEnvelope().payloadType(), e.getMessage()),
        0
      ), inputMessage, timestamp, clientSessionId);
      return Optional.empty();
    }
  }

}
