package io.alv.core;

public sealed interface IntIndex<T> extends Index {

  int extractKey(T model);
}
