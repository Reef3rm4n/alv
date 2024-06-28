package io.alv.core.handler;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.ObjectHashSet;

public final class MessageContext<M> {

  public final M message;
  public final long timestamp;
  public final ObjectHashSet<Object> sendBuffer = new ObjectHashSet<>(5);
  public final Long2ObjectHashMap<Object> schedule = new Long2ObjectHashMap<>(1, 0.65F, true);

  MessageContext(M message, long timestamp) {
    this.message = message;
    this.timestamp = timestamp;
  }

  public <T> void send(T message) {
    sendBuffer.add(message);
  }

  public <T> void schedule(long timestamp, T message) {
    schedule.put(timestamp, message);
  }


}
