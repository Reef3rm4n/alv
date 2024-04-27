package io.alv.core.handler.messages.output;

import io.alv.core.handler.messages.objects.MessageEnvelope;

public record Event(
  long timestamp,
  long snowflake,
  MessageEnvelope payload
) implements Output {
}
