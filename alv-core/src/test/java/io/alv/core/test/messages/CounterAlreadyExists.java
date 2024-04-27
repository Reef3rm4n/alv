package io.alv.core.test.messages;

import io.alv.core.Message;

@Message
public record CounterAlreadyExists(
  String id,
  int currentCount
) {
}
