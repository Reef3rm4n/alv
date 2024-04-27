package io.alv.core.test.messages;

import io.alv.core.Message;

@Message
public record CounterNotFound(
  String id
) {
}
