package io.alv.core.handler.messages.handles;

import io.alv.core.handler.messages.output.Ack;
import io.alv.core.handler.messages.output.ErrorMessage;
import io.alv.core.handler.messages.output.Event;

import java.util.function.Consumer;

public interface MessageOffer<M> {

  M message();
  Consumer<Event> onEvent();
  Consumer<Ack> onCompletion();
  Consumer<ErrorMessage> onError();
}
