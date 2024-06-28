package io.alv.core;

public sealed interface StringIndex<T> extends Index {

  String extractKey(T model);
}
