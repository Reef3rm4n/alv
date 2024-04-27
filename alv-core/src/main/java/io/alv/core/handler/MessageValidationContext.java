package io.alv.core.handler;

import io.alv.core.cluster.storage.Lmdb;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import org.agrona.DirectBuffer;
import org.agrona.collections.ObjectHashSet;
import org.lmdbjava.Txn;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class MessageValidationContext<M> {

  public final Lmdb lmdb;
  public final M message;
  public final long timestamp;
  public final List<ConstraintViolation> violations = new ArrayList<>(1);
  private final Txn<DirectBuffer> txn;

  public MessageValidationContext(
    Txn<DirectBuffer> txn,
    M message,
    long timestamp,
    Lmdb lmdb
  ) {
    this.txn = txn;
    this.message = message;
    this.timestamp = timestamp;
    this.lmdb = lmdb;
  }

  public void violation(ConstraintViolation violation) {
    violations.add(violation);
  }
  public <V> List<V> search(Predicate<V> valuePredicate, Class<V> clazz) {
    return lmdb.search(txn, valuePredicate, clazz);
  }

  public <V> Optional<V> get(Txn<DirectBuffer> txn, long key, Class<V> clazz) {
    return lmdb.get(txn, key, clazz);
  }

  public <V> Optional<V> get(Txn<DirectBuffer> txn, int key, Class<V> clazz) {
    return lmdb.get(txn, key, clazz);
  }

  public <V> Optional<V> get(String key, Class<V> clazz) {
    return lmdb.get(txn, key, clazz);
  }

}
