package io.alv.core.test;

import io.alv.core.cluster.storage.Lmdb;
import io.alv.core.test.model.Counter;
import org.agrona.DirectBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lmdbjava.*;

import java.util.UUID;
import java.util.stream.IntStream;

class LmdbTest {
  private static final Lmdb lmdb = new Lmdb();

  @AfterAll
  static void stop() {
    lmdb.close();
  }

  @BeforeEach
  void cleanUp() {
    lmdb.cleanUp(Counter.class);
  }

  @Test
  void lmdbStringEntryTestMany() {
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      IntStream.range(0, 10).forEach(i -> put(txn));
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      final var result = lmdb.search(txn, v -> v.current() == 1, Counter.class);
      Assertions.assertEquals(10, result.size());
    }
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      lmdb.delete(
        txn,
        v -> v.current() == 1, Counter.class
      );
      txn.commit();
    }
  }

  private static String put(Txn<DirectBuffer> txn) {
    return put(txn, UUID.randomUUID().toString(), 1);
  }

  private static String put(Txn<DirectBuffer> txn, String key, int counter) {
    final var result = lmdb.put(
      txn,
      key,
      new Counter(counter)
    );
    Assertions.assertTrue(result);
    return key;
  }

  @Test
  void lmdbStringEntryTest() {
    String key;
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      key = put(txn);
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      final var result = lmdb.search(txn, v -> v.current() == 1, Counter.class);
      Assertions.assertEquals(1, result.size());
      Assertions.assertEquals(1, result.stream().findFirst().orElseThrow().current());
      System.out.println("Results :" + result);
      final var counter = lmdb.get(txn, key, Counter.class).orElse(null);
      Assertions.assertNotNull(counter);
      Assertions.assertEquals(1, counter.current());

    }
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      Assertions.assertTrue(
        lmdb.delete(
          txn,
          key,
          Counter.class
        )
      );
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      Assertions.assertNull(lmdb.get(txn, key, Counter.class).orElse(null));
    }
  }

  @Test
  void lmdbStringEntryOverwriteTest() {
    String key;
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      key = put(txn);
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      key = put(txn, key, 10);
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      final var counter = lmdb.get(txn, key, Counter.class).orElse(null);
      System.out.println("Counter :" + counter);
      Assertions.assertNotNull(counter);
      Assertions.assertEquals(10, counter.current());
    }
  }

  @Test
  void lmdbIntEntryTest() {
    final int key = 666;
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      final var result = lmdb.put(
        txn,
        key,
        new Counter(1)
      );
      Assertions.assertTrue(result);
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      final var counter = lmdb.get(txn, key, Counter.class).orElse(null);
      Assertions.assertNotNull(counter);
      Assertions.assertEquals(1, counter.current());
      final var result = lmdb.search(txn, v -> v.current() == 1, Counter.class);
      Assertions.assertEquals(1, result.size());
      Assertions.assertEquals(1, result.stream().findFirst().orElseThrow().current());
    }

    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      lmdb.delete(
        txn,
        key,
        Counter.class
      );
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      Assertions.assertNull(lmdb.get(txn, key, Counter.class).orElse(null));
    }
  }

  @Test
  void lmdbLongEntryTest() {
    final long key = 999L;
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      final var result = lmdb.put(
        txn,
        key,
        new Counter(1)
      );
      Assertions.assertTrue(result);
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      final var counter = lmdb.get(txn, key, Counter.class).orElse(null);
      Assertions.assertNotNull(counter);
      Assertions.assertEquals(1, counter.current());
      final var result = lmdb.search(txn, v -> v.current() == 1, Counter.class);
      Assertions.assertEquals(1, result.size());
      Assertions.assertEquals(1, result.stream().findFirst().orElseThrow().current());
    }
    try (Txn<DirectBuffer> txn = lmdb.txnWrite()) {
      Assertions.assertTrue(lmdb.delete(
          txn,
          key,
          Counter.class
        )
      );
      txn.commit();
    }
    try (Txn<DirectBuffer> txn = lmdb.txnRead()) {
      Assertions.assertNull(lmdb.get(txn, key, Counter.class).orElse(null));
    }
  }
}
