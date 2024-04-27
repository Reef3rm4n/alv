package io.alv.core.cluster.storage;

import io.alv.core.handler.Models;
import io.alv.core.handler.SnapshotPublisher;
import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.storage.Int2ObjectEntry;
import io.alv.core.handler.messages.storage.Long2ObjectEntry;
import io.alv.core.handler.messages.storage.StorageEntry;
import io.alv.core.handler.messages.storage.String2ObjectEntry;
import org.agrona.DirectBuffer;
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.lmdbjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.alv.core.handler.ClusterConfiguration.*;
import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.DirectBufferProxy.PROXY_DB;
import static org.lmdbjava.EnvFlags.*;

public class Lmdb {

  private static final Logger LOGGER = LoggerFactory.getLogger(Lmdb.class);
  private final Env<DirectBuffer> env;
  private final ByteBuffer keyByteBuffer;
  private final UnsafeBuffer keyBuffer;
  private final Object2ObjectHashMap<String, Dbi<DirectBuffer>> dbis = new Object2ObjectHashMap<>();
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);

  public Lmdb() {
    final var models = ServiceLoader.load(Models.class).stream()
      .map(ServiceLoader.Provider::get)
      .map(Models::models)
      .map(Map::keySet)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());
    this.env = Env.create(PROXY_DB)
      .setMapSize(
        ByteConverter.convertToBytes(CONFIG.getOptionalValue("lmdb.size", String.class)
          .orElse("1gb")
        )
      )
      .setMaxDbs(models.size())
      .open(
        new File(CONFIG
          .getOptionalValue("lmdb.dir", String.class)
          .orElse("/tmp/lmdb")
        ),
        List.of(MDB_WRITEMAP, EnvFlags.MDB_NOLOCK).toArray(EnvFlags[]::new)
      );
    this.keyByteBuffer = ByteBuffer.allocateDirect(env.getMaxKeySize());
    this.keyBuffer = new UnsafeBuffer(keyByteBuffer);
    models.forEach(model -> {
        LOGGER.info("Opening dbi for {}", model.getName());
        dbis.put(
          model.getName(),
          env.openDbi(model.getSimpleName(), MDB_CREATE)
        );
      }
    );
  }

  private <V> Dbi<DirectBuffer> resolveDbi(Class<V> clazz) {
    return resolveDbi(clazz.getName());
  }

  private Dbi<DirectBuffer> resolveDbi(String name) {
    return dbis.get(name);
  }

  public void close() {
    dbis.values().forEach(dbi -> {
        try (Txn<DirectBuffer> txn = env.txnWrite()) {
          dbi.drop(txn);
          txn.commit();
        }
      }
    );
    dbis.values().forEach(Dbi::close);
    env.close();
  }

  public void cleanUp(Class<?> dbiClass) {
    try (final var txn = env.txnWrite()) {
      resolveDbi(dbiClass).drop(txn);
      txn.commit();
    }
  }

  public void snapshot(SnapshotPublisher snapshotPublisher) {
    try (var txn = env.txnRead()) {
      dbis.values().forEach(
        dbi -> {
          try (var cursor = dbi.openCursor(txn)) {
            while (cursor.next()) {
              final var buffer = cursor.val();
              snapshotPublisher.send(
                decoderContext.get().decodeEntry(buffer, 0, buffer.capacity())
              );
            }
          }
        }
      );
    }
  }

  public Txn<DirectBuffer> txnRead() {
    return env.txnRead();
  }

  public Txn<DirectBuffer> txnWrite() {
    return env.txnWrite();
  }

  public void write(Int2ObjectEntry entry) {
    keyBuffer.wrap(keyByteBuffer);
    try (var txn = env.txnWrite()) {
      keyBuffer.putInt(0, entry.key(), ByteOrder.BIG_ENDIAN);
      keyBuffer.wrap(keyBuffer, 0, Integer.BYTES);
      assert put(txn, entry);
      txn.commit();
    }
  }

  public void write(Long2ObjectEntry entry) {
    keyBuffer.wrap(keyByteBuffer);
    try (var txn = env.txnWrite()) {
      keyBuffer.putLong(0, entry.key(), ByteOrder.BIG_ENDIAN);
      keyBuffer.wrap(keyBuffer, 0, Long.BYTES);
      assert put(txn, entry);
      txn.commit();
    }
  }

  public void write(String2ObjectEntry entry) {
    keyBuffer.wrap(keyByteBuffer);
    try (var txn = env.txnWrite()) {
      final var length = keyBuffer.putStringUtf8(0, entry.key());
      keyBuffer.wrap(keyBuffer, 0, length);
      assert put(txn, entry);
      txn.commit();
    }
  }

  public <V> boolean put(Txn<DirectBuffer> txn, String key, V value) {
    keyBuffer.wrap(keyByteBuffer);
    final var length = keyBuffer.putStringUtf8(0, key);
    keyBuffer.wrap(keyBuffer, 0, length);
    final var entry = new String2ObjectEntry(
      key,
      MessageEnvelopeCodec.serialize(value)
    );
    return put(txn, entry);
  }

  public <V> boolean put(Txn<DirectBuffer> txn, long key, V value) {
    keyBuffer.wrap(keyByteBuffer);
    keyBuffer.putLong(0, key, ByteOrder.BIG_ENDIAN);
    keyBuffer.wrap(keyBuffer, 0, Long.BYTES);
    final var entry = new Long2ObjectEntry(
      key,
      MessageEnvelopeCodec.serialize(value)
    );
    return put(txn, entry);
  }

  public <V> boolean put(Txn<DirectBuffer> txn, int key, V value) {
    keyBuffer.wrap(keyByteBuffer);
    keyBuffer.putInt(0, key, ByteOrder.BIG_ENDIAN);
    keyBuffer.wrap(keyBuffer, 0, Integer.BYTES);
    final var entry = new Int2ObjectEntry(
      key,
      MessageEnvelopeCodec.serialize(value)
    );
    return put(txn, entry);
  }


  private boolean put(Txn<DirectBuffer> txn, StorageEntry messagePayload) {
    final var unsafeBuffer = decoderContext.get().encode(messagePayload);
    return resolveDbi(messagePayload.payload().payloadType())
      .put(txn, keyBuffer, unsafeBuffer);
  }

  public <V> Optional<V> get(Txn<DirectBuffer> txn, long key, Class<V> clazz) {
    keyBuffer.wrap(keyByteBuffer);
    keyBuffer.putLong(0, key, ByteOrder.BIG_ENDIAN);
    keyBuffer.wrap(keyBuffer, 0, Long.BYTES);
    return Optional.ofNullable(get(txn, clazz));
  }

  public <V> Optional<V> get(Txn<DirectBuffer> txn, int key, Class<V> clazz) {
    keyBuffer.wrap(keyByteBuffer);
    keyBuffer.putInt(0, key, ByteOrder.BIG_ENDIAN);
    keyBuffer.wrap(keyBuffer, 0, Integer.BYTES);
    return Optional.ofNullable(get(txn, clazz));
  }

  public <V> Optional<V> get(Txn<DirectBuffer> txn, String key, Class<V> clazz) {
    keyBuffer.wrap(keyByteBuffer);
    final var length = keyBuffer.putStringUtf8(0, key);
    keyBuffer.wrap(keyBuffer, 0, length);
    return Optional.ofNullable(get(txn, clazz));
  }

  private <V> V get(Txn<DirectBuffer> txn, Class<V> clazz) {
    final var result = resolveDbi(clazz).get(txn, keyBuffer);
    if (Objects.nonNull(result)) {
      return MessageEnvelopeCodec.deserialize(
        decoderContext.get().decodeEntry(
          result,
          0,
          result.capacity()
        ).payload()
      );
    }
    return null;
  }

  public <V> boolean delete(Txn<DirectBuffer> txn, long key, Class<V> clazz) {
    keyBuffer.wrap(keyByteBuffer);
    keyBuffer.putLong(0, key, ByteOrder.BIG_ENDIAN);
    keyBuffer.wrap(keyBuffer, 0, Long.BYTES);
    return delete(txn, clazz);
  }

  public <V> boolean delete(Txn<DirectBuffer> txn, int key, Class<V> clazz) {
    keyBuffer.wrap(keyByteBuffer);
    keyBuffer.putInt(0, key, ByteOrder.BIG_ENDIAN);
    keyBuffer.wrap(keyBuffer, 0, Integer.BYTES);
    return delete(txn, clazz);
  }

  public <V> boolean delete(Txn<DirectBuffer> txn, String key, Class<V> clazz) {
    keyBuffer.wrap(keyByteBuffer);
    final var length = keyBuffer.putStringUtf8(0, key);
    keyBuffer.wrap(keyBuffer, 0, length);
    return delete(txn, clazz);
  }

  private <V> boolean delete(Txn<DirectBuffer> txn, Class<V> clazz) {
    return resolveDbi(clazz)
      .delete(txn, keyBuffer);
  }

  public <V> List<V> delete(Txn<DirectBuffer> txn, Predicate<V> valuePredicate, Class<V> clazz) {
    final var result = new ArrayList<V>(10);
    try (var cursor = resolveDbi(clazz).openCursor(txn)) {
      while (cursor.next()) {
        final var value = MessageEnvelopeCodec.<V>deserialize(
          decoderContext.get().decodeEntry(
              cursor.val(),
              0,
              cursor.val().capacity()
            )
            .payload()
        );
        if (valuePredicate.test(value)) {
          resolveDbi(clazz).delete(txn, cursor.key());
          result.add(value);
        }
      }
    }
    return result;
  }

  public <V> List<V> search(Txn<DirectBuffer> txn, Predicate<V> valuePredicate, Class<V> clazz) {
    final var result = new ArrayList<V>(10);
    try (var cursor = resolveDbi(clazz).openCursor(txn)) {
      while (cursor.next()) {
        final var value = MessageEnvelopeCodec.<V>deserialize(
          decoderContext.get().decodeEntry(
              cursor.val(),
              0,
              cursor.val().capacity()
            )
            .payload()
        );
        if (valuePredicate.test(value)) {
          result.add(value);
        }
      }
    }
    return result;
  }

}
