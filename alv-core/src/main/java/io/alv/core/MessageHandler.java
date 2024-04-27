package io.alv.core;

import io.alv.core.handler.MessageHandlerContext;
import io.alv.core.handler.MessageValidationContext;

public interface MessageHandler<M> {
  void onValidation(MessageValidationContext<M> session);

  void onMessage(MessageHandlerContext<M> session);


}
