package io.alv.core.handler.messages.input;

public record InputMessage(
  long snowflake,
  io.alv.core.handler.messages.objects.MessageEnvelope messageEnvelope
) implements Input {

}
