package io.alv.core.test.messages;

import io.alv.core.Message;

@Message
public record CounterIncremented(
  String id,
  int count
) {
}
