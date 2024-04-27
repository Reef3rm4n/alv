package io.alv.core.test.messages;

import io.alv.core.Message;

@Message
public record IncrementCounter(
  boolean failValidation,
  boolean failHandling,
  String id
) {
}
