package io.alv.core.test.messages;

import io.alv.core.Message;

@Message
public record CounterCreated(
  String id
) {
}
