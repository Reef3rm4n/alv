package io.alv.core.handler.messages.output;

public record Ack(
  long snowflake,
  long timestamp
) implements Output {
}
