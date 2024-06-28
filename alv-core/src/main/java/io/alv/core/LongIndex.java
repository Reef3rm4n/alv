package io.alv.core;

public abstract non-sealed class LongIndex<T> implements Index {

  abstract long extractKey(T model);

}
