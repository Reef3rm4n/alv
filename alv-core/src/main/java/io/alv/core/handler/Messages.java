package io.alv.core.handler;

import io.alv.core.Encoding;

import java.util.Map;

public interface Messages {

  Map<Class<?>, Encoding> messages();
}
