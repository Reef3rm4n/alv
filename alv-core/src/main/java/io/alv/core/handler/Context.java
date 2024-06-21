package io.alv.core.handler;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.ObjectHashSet;

public class Context<M> {

  public final M message;
  public final long timestamp;
  public final ObjectHashSet<Object> broadcast = new ObjectHashSet<>(5);
  public final ObjectHashSet<Object> unicast = new ObjectHashSet<>(1);
  public final Long2ObjectHashMap<Object> schedule = new Long2ObjectHashMap<>();

  public final ReadWriteState state;

  public Context(M message, long timestamp, ReadWriteState state) {
    this.message = message;
    this.timestamp = timestamp;
    this.state = state;
  }

  public <T> void unicast(T message) {
    unicast.add(message);
  }

  public <T> void broadcast(T message) {
    broadcast.add(message);
  }

  public <T> void schedule(long timestamp, T message) {
    schedule.put(timestamp, message);
  }


}
