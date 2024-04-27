package io.alv.core.test.messages;

import io.alv.core.Message;

@Message
public record DecrementCounter(
  boolean failValidation,
  boolean failHandling,
  String id
) {
}
