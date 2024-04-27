package io.alv.core.test.messages;


import io.alv.core.Encoding;
import io.alv.core.Message;
import io.alv.core.Model;

@Message
public record CounterDecremented(
  String id
) {
}
