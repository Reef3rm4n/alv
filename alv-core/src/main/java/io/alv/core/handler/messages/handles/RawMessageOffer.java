package io.alv.core.handler.messages.handles;

import io.alv.core.handler.messages.output.Ack;
import io.alv.core.handler.messages.output.Event;
import io.alv.core.handler.messages.output.ErrorMessage;

import java.util.function.Consumer;

public record RawMessageOffer<M>(
  M message,
  Consumer<Event> onEvent,
  Consumer<Ack> onCompletion,
  Consumer<ErrorMessage> onError
) implements MessageOffer<M> {
}
