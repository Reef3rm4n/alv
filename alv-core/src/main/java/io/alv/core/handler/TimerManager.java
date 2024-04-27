package io.alv.core.handler;

import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.input.InputMessage;
import io.aeron.cluster.service.Cluster;
import org.agrona.collections.Long2ObjectHashMap;

public class TimerManager {

  private final Long2ObjectHashMap<InputMessage> scheduledCommands = new Long2ObjectHashMap<>();

  private final Cluster cluster;

  public TimerManager(
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
    scheduledCommands.put(snowflake, encodedCommand);
    cluster.scheduleTimer(snowflake, deadline);
  }

  public InputMessage get(long correlationKey) {
    return scheduledCommands.get(correlationKey);
  }

}
