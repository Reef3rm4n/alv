package io.alv.core;

import io.alv.core.handler.Context;
import io.alv.core.handler.ValidationContext;

public interface MessageHandler<M> {

  default void onValidation(ValidationContext<M> session) {
    // do nothing
  }

  void onMessage(Context<M> session);

}
