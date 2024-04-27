package io.alv.core.cluster.storage;


import io.alv.core.handler.SnapshotPublisher;
import io.alv.core.handler.messages.storage.StorageEntry;
import org.agrona.collections.*;

public class AgronaObjectStore {
  private final Object2ObjectHashMap<Class<?>, Object2ObjectHashMap<?, ?>> object2ObjectHashMap = new Object2ObjectHashMap<>();
  private final Object2ObjectHashMap<Class<?>, Int2ObjectHashMap<?>> int2ObjectHashMap = new Object2ObjectHashMap<>(2, 0.65F);
  private final Object2ObjectHashMap<Class<?>, Long2ObjectHashMap<?>> long2ObjectHashMap = new Object2ObjectHashMap<>(2, 0.65F);
  private final Object2ObjectHashMap<Class<?>, ObjectHashSet<?>> objectHashSet = new Object2ObjectHashMap<>(2, 0.65F);
  private final Object2ObjectHashMap<Class<?>, Object2IntCounterMap<?>> object2IntCounterMap = new Object2ObjectHashMap<>(2, 0.65F);
  private final Object2ObjectHashMap<Class<?>, Object2LongCounterMap<?>> object2LongCounterMap = new Object2ObjectHashMap<>(2, 0.65F);
  private final Object2ObjectHashMap<Class<?>, BiInt2ObjectMap<?>> biInt2ObjectMap = new Object2ObjectHashMap<>(2, 0.65F);


  public <K, V> Object2ObjectHashMap<K, V> object2ObjectHashMap(Class<K> keyClass, Class<V> valueClass) {
    return (Object2ObjectHashMap<K, V>) object2ObjectHashMap.computeIfAbsent(keyClass, k -> new Object2ObjectHashMap<>());
  }


  public <V> Int2ObjectHashMap<V> int2ObjectHashMap(Class<V> valueClass) {
    return (Int2ObjectHashMap<V>) int2ObjectHashMap.computeIfAbsent(valueClass, k -> new Int2ObjectHashMap<>());
  }


  public <V> Long2ObjectHashMap<V> long2ObjectHashMap(Class<V> valueClass) {
    return (Long2ObjectHashMap<V>) long2ObjectHashMap.computeIfAbsent(valueClass, k -> new Long2ObjectHashMap<>());
  }


  public <V> ObjectHashSet<V> objectHashSet(Class<V> valueClass) {
    return (ObjectHashSet<V>) objectHashSet.computeIfAbsent(valueClass, k -> new ObjectHashSet<>());
  }


  public <V> Object2IntCounterMap<V> object2IntCounterMap(Class<V> valueClass) {
    return (Object2IntCounterMap<V>) object2IntCounterMap.computeIfAbsent(valueClass, k -> new Object2IntCounterMap<>(0));
  }


  public <V> Object2LongCounterMap<V> object2LongCounterMap(Class<V> valueClass) {
    return (Object2LongCounterMap<V>) object2LongCounterMap.computeIfAbsent(valueClass, k -> new Object2LongCounterMap<>(0));
  }

  public <V> BiInt2ObjectMap<V> biInt2ObjectMap(Class<V> valueClass) {
    return (BiInt2ObjectMap<V>) biInt2ObjectMap.computeIfAbsent(valueClass, k -> new BiInt2ObjectMap<>());
  }

  public void snapshot(SnapshotPublisher snapshotPublisher) {

  }

  public void load(StorageEntry fragment) {

  }

}
