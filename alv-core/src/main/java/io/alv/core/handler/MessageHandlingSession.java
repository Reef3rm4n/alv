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
import org.agrona.concurrent.UnsafeBuffer;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MessageHandlingSession<M> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlingSession.class);
  private final MessageHandler<M> messageHandler;
  private final ClientSessionManager clientSessionManager;
  private final TimerManager timerManager;
  private final Lmdb lmdb;


  public MessageHandlingSession(
    TimerManager timerManager,
    ClientSessionManager clientSessionManager,
    MessageHandler<M> messageHandler,
    Lmdb lmdb
  ) {
    this.timerManager = timerManager;
    this.clientSessionManager = clientSessionManager;
    this.messageHandler = messageHandler;
    this.lmdb = lmdb;
  }

  public void onMessage(InputMessage inputMessage, long timestamp, long clientSessionId, long logPosition) {
    LOGGER.debug("Handling message logPosition={} snowflake={} sessionId={} timestamp={}", logPosition, inputMessage.snowflake(), clientSessionId, timestamp);
    try (var txn = lmdb.txnWrite()) {
      deserializeMessage(inputMessage, timestamp, clientSessionId).ifPresent(
        message -> validate(message, timestamp, txn)
          .ifPresentOrElse(
            error -> sendError(error, inputMessage, timestamp, clientSessionId),
            () -> handleMessage(message, inputMessage, timestamp, clientSessionId, txn)
          )
      );
    } catch (Exception e) {
      LOGGER.error("Error handling message", e);
      sendError(new Error(ErrorType.HANDLER_EXCEPTION, e.getMessage(), 0), inputMessage, timestamp, clientSessionId);
    }
  }

  private void sendError(Error error, InputMessage inputMessage, long timestamp, long clientSessionId) {
    clientSessionManager.send(clientSessionId, new ErrorMessage(timestamp, inputMessage.snowflake(), error));
  }

  private Optional<Error> validate(M decodedCommand, long timestamp, Txn<DirectBuffer> txn) {
    final var session = new MessageValidationContext<>(txn, decodedCommand, timestamp, lmdb);
    messageHandler.onValidation(session);
    if (!session.violations.isEmpty()) {
      return Optional.of(new Error(
          ErrorType.CONSTRAINT_VIOLATION,
          session.violations.stream()
            .map(ConstraintViolation::message)
            .collect(Collectors.joining(",", "[", "]")),
          session.violations.stream().map(ConstraintViolation::code).findFirst().orElseThrow()
        )
      );
    } else {
      return Optional.empty();
    }
  }

  private void handleMessage(M message, InputMessage envelope, long timestamp, long clientSessionId, Txn<DirectBuffer> txn) {
    try {
      final var session = new MessageHandlerContext<>(txn, message, timestamp, lmdb);
      messageHandler.onMessage(session);
      if (!session.schedule.isEmpty()) {
        session.schedule.forEach(
          (deadline, cmd) -> timerManager
            .schedule(envelope.snowflake(), deadline, cmd)
        );
      }
      txn.commit();
      sideEffects(envelope, timestamp, clientSessionId, session);
      ack(envelope, timestamp, clientSessionId);
    } catch (Exception e) {
      LOGGER.error("Error handling message", e);
      clientSessionManager.send(clientSessionId, new ErrorMessage(timestamp, envelope.snowflake(), new Error(
        ErrorType.HANDLER_EXCEPTION,
        e.getMessage(),
        0
      )));
    }
  }

  private void sideEffects(InputMessage envelope, long timestamp, long clientSessionId, MessageHandlerContext<M> session) {
    if (!session.send.isEmpty()) {
      session.send.forEach(reply -> reply(reply, envelope, timestamp, clientSessionId));
    }
    if (!session.broadcast.isEmpty()) {
      session.broadcast.forEach(event -> broadcast(event, envelope, timestamp, clientSessionId));
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

  private void ack(InputMessage inputMessage, long timestamp, long clientSessionId) {
    clientSessionManager.send(clientSessionId, new Ack(inputMessage.snowflake(), timestamp));
  }

  private void reply(Object response, InputMessage envelope, long timestamp, long clientSessionId) {
    serializePayload(response, envelope, timestamp, clientSessionId)
      .ifPresent(payload -> clientSessionManager.send(clientSessionId, new Event(timestamp, envelope.snowflake(), payload)));
  }

  private Optional<MessageEnvelope> serializePayload(Object response, InputMessage inputMessage, long timestamp, long clientSessionId) {
    try {
      return Optional.of(MessageEnvelopeCodec.serialize(response));
    } catch (Exception exception) {
      sendError(new Error(
        ErrorType.SERIALIZATION_EXCEPTION,
        "Error serializing event %s emitted during handling of command %s error=%s".formatted(response.getClass().getSimpleName(), inputMessage.messageEnvelope().payloadType(), exception.getMessage()),
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
        "Unable to decode command %s error=%s".formatted(inputMessage.messageEnvelope().payloadType(), e.getMessage()),
        0
      ), inputMessage, timestamp, clientSessionId);
      return Optional.empty();
    }
  }

}
