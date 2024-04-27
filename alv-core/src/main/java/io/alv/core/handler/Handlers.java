package io.alv.core.handler;

import io.alv.core.MessageHandler;

import java.util.Set;

public interface Handlers {
  Set<MessageHandler<?>> handlers();

}
