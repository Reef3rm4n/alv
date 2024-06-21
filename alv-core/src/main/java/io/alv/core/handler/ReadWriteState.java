package io.alv.core.handler;

import io.alv.core.cluster.storage.Lmdb;
import org.agrona.DirectBuffer;
import org.lmdbjava.Txn;

public class ReadWriteState extends ReadOnlyState {

  public ReadWriteState(Txn<DirectBuffer> txn, Lmdb lmdb) {
    super(txn, lmdb);
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
