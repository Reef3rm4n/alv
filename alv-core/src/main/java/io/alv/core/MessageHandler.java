package io.alv.core;

import io.alv.core.handler.MessageContext;
import io.alv.core.handler.ReadOnlyMemoryStore;
import io.alv.core.handler.ReadWriteMemoryStore;
import io.alv.core.handler.ValidationContext;

public interface MessageHandler<M> {

  default void onValidation(ValidationContext<M> session, ReadOnlyMemoryStore memoryStore) {
    // do nothing
  }

  void onMessage(MessageContext<M> session, ReadWriteMemoryStore memoryStore);

}
