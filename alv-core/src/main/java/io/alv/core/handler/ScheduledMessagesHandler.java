package io.alv.core.handler;

import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.input.InputMessage;
import io.aeron.cluster.service.Cluster;
import org.agrona.collections.Long2ObjectHashMap;

public class ScheduledMessagesHandler {

  private final Long2ObjectHashMap<InputMessage> scheduledMessages = new Long2ObjectHashMap<>();

  private final Cluster cluster;

  public ScheduledMessagesHandler(
    Cluster cluster
  ) {
    this.cluster = cluster;
  }

  public void schedule(long snowflake, long deadline, Object command) {
    final var encodedCommand = new InputMessage(
      snowflake,
      MessageEnvelopeCodec.serialize(
        command
      )
    );
    scheduledMessages.put(snowflake, encodedCommand);
    cluster.scheduleTimer(snowflake, deadline);
  }

  public InputMessage get(long correlationKey) {
    return scheduledMessages.get(correlationKey);
  }

}
