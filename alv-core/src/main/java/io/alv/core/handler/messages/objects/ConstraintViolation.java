package io.alv.core.handler.messages.objects;

public record ConstraintViolation(
  String message,
  int code
) {
}
