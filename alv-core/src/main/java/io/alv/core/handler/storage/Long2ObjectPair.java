package io.alv.core.handler.storage;

public record Long2ObjectPair<V>(
  long key,
  V value
) {
}
