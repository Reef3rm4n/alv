package io.alv.core.handler;

import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.handles.BroadcastSubscription;
import io.alv.core.handler.messages.handles.MessageOffer;
import io.alv.core.handler.messages.output.ErrorMessage;
import io.alv.core.handler.messages.output.Event;
import io.alv.core.handler.messages.output.Ack;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.AdminRequestType;
import io.aeron.cluster.codecs.AdminResponseCode;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.ObjectHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class ClusterEgressListener implements EgressListener {
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);
  private final Long2ObjectHashMap<MessageOffer<?>> messageHandles = new Long2ObjectHashMap<>();
  private final Long2ObjectHashMap<Consumer<AdminResponseCode>> adminHandles = new Long2ObjectHashMap<>();
  private final ObjectHashSet<BroadcastSubscription> broadcastSubscriptions = new ObjectHashSet<>(2);
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterEgressListener.class);

  @Override
  public void onMessage(long clusterSessionId, long timestamp, DirectBuffer buffer, int offset, int length, Header header) {
    final var output = decoderContext.get().decodeOutput(buffer, offset, length);
    if (output instanceof Event event) {
      handleReply(event);
      handleBroadcast(event);
    } else if (output instanceof Ack ack) {
      handleAck(ack);
    } else if (output instanceof ErrorMessage errorMessage) {
      handleError(errorMessage);
    }
  }

  private void handleError(ErrorMessage errorMessage) {
    Objects.requireNonNullElse(
        messageHandles.remove(errorMessage.snowflake()).onError(),
        error -> {
        }
      )
      .accept(errorMessage);
  }

  private void handleAck(Ack ack) {
    Objects.requireNonNullElse(
        messageHandles.remove(ack.snowflake()).onCompletion(),
        e -> {
        }
      )
      .accept(ack);
  }

  private void handleBroadcast(Event event) {
    if (event.snowflake() == 0) {
      broadcastSubscriptions.forEach(c -> c.onEvent(event));
    }
  }

  private void handleReply(Event event) {
    if (messageHandles.containsKey(event.snowflake())) {
      messageHandles.get(event.snowflake()).onEvent().accept(event);
    }
  }

  @Override
  public void onSessionEvent(long correlationId, long clusterSessionId, long leadershipTermId, int leaderMemberId, EventCode code, String detail) {
    LOGGER.info("Session event: correlationId={} clusterSessionId={} leadershipTermId={} leaderMemberId={} code={} detail={}", correlationId, clusterSessionId, leadershipTermId, leaderMemberId, code, detail);
  }

  @Override
  public void onNewLeader(long clusterSessionId, long leadershipTermId, int leaderMemberId, String ingressEndpoints) {
    LOGGER.info("New leader: clusterSessionId={} leadershipTermId={} leaderMemberId={} ingressEndpoints={}", clusterSessionId, leadershipTermId, leaderMemberId, ingressEndpoints);
  }

  @Override
  public void onAdminResponse(long clusterSessionId, long correlationId, AdminRequestType requestType, AdminResponseCode responseCode, String message, DirectBuffer payload, int payloadOffset, int payloadLength) {
    LOGGER.info("Admin response: clusterSessionId={} correlationId={} requestType={} responseCode={} message={} payload={} payloadOffset={} payloadLength={}", clusterSessionId, correlationId, requestType, responseCode, message, payload, payloadOffset, payloadLength);
    adminHandles.get(correlationId).accept(responseCode);
  }


  public void register(long snowflake, MessageOffer<?> rawMessageOffer) {
    messageHandles.put(snowflake, rawMessageOffer);
  }

  public void register(BroadcastSubscription eventConsumer) {
    broadcastSubscriptions.add(eventConsumer);
  }

  public void unregister(long snowflake) {
    messageHandles.remove(snowflake);
  }

  public void unregister(BroadcastSubscription snowflake) {
    broadcastSubscriptions.removeIf(s -> s.getClass().isAssignableFrom(snowflake.getClass()));
  }
  public void registerAdminMessageConsumer(long correlationId, Consumer<AdminResponseCode> responseCodeConsumer) {
    adminHandles.put(correlationId, responseCodeConsumer);
  }
}
