package io.alv.core.handler.messages.input;

public sealed interface Input permits InputMessage {

  long snowflake();
  io.alv.core.handler.messages.objects.MessageEnvelope messageEnvelope();
}
