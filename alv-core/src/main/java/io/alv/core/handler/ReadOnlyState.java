package io.alv.core.handler;

import io.alv.core.cluster.storage.Lmdb;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.collections.ObjectHashSet;
import org.lmdbjava.Txn;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ReadOnlyState {

  protected final Txn<DirectBuffer> txn;
  protected final Lmdb lmdb;

  public ReadOnlyState(Txn<DirectBuffer> txn, Lmdb lmdb) {
    this.lmdb = lmdb;
    this.txn = txn;
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

  public <V> List<V> range(int from, int to, Class<V> vClass) {
    return lmdb.range(txn, from, to, vClass);
  }

  public <V> List<V> range(String from, String to, Class<V> vClass) {
    return lmdb.range(txn, from, to, vClass);
  }

  public <V> List<V> range(long from, long to, Class<V> vClass) {
    return lmdb.range(txn, from, to, vClass);
  }

}
