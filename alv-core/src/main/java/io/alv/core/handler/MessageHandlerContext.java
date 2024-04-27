package io.alv.core.handler;

import io.alv.core.cluster.storage.Lmdb;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.ObjectHashSet;
import org.lmdbjava.Txn;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class MessageHandlerContext<M> {

  public final M message;
  public final long timestamp;
  public final ObjectHashSet<Object> broadcast = new ObjectHashSet<>(5);
  public final ObjectHashSet<Object> send = new ObjectHashSet<>(1);
  public final Long2ObjectHashMap<Object> schedule = new Long2ObjectHashMap<>();
  private final Txn<DirectBuffer> txn;
  public final Lmdb lmdb;

  public MessageHandlerContext(Txn<DirectBuffer> txn, M message, long timestamp, Lmdb lmdb) {
    this.message = message;
    this.timestamp = timestamp;
    this.lmdb = lmdb;
    this.txn = txn;
  }

  public <T> void reply(T rsponse) {
    send.add(rsponse);
  }

  public <T> void broadcast(T event) {
    broadcast.add(event);
  }

  public <T> void schedule(long timestamp, T message) {
    schedule.put(timestamp, message);
  }

  public <V> List<V> search(Predicate<V> valuePredicate, Class<V> clazz) {
    return lmdb.search(txn, valuePredicate, clazz);
  }

  public <V> Optional<V> get(long key, Class<V> clazz) {
    return lmdb.get(txn, key, clazz);
  }

  public <V> Optional<V> get(int key, Class<V> clazz) {
    return lmdb.get(txn, key, clazz);
  }

  public <V> Optional<V> get(String key, Class<V> clazz) {
    return lmdb.get(txn, key, clazz);
  }

  public <V> boolean put(String key, V value) {
    return lmdb.put(txn, key, value);
  }

  public <V> boolean put(long key, V value) {
    return lmdb.put(txn, key, value);
  }

  public <V> boolean put(int key, V value) {
    return lmdb.put(txn, key, value);
  }

  public <V> boolean delete(String key, Class<V> clazz) {
    return lmdb.delete(txn, key, clazz);
  }

  public <V> boolean delete(long key, Class<V> clazz) {
    return lmdb.delete(txn, key, clazz);
  }

  public <V> boolean delete(int key, Class<V> clazz) {
    return lmdb.delete(txn, key, clazz);
  }


}
