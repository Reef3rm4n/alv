package io.alv.core.handler.messages.output;

import io.alv.core.handler.messages.objects.Error;

public record ErrorMessage(
  long timestamp,
  long snowflake,
  Error error
) implements Output {
}
