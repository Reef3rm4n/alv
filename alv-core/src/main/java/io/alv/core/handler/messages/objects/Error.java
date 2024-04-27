package io.alv.core.handler.messages.objects;

import io.alv.core.ErrorType;

public record Error(
  ErrorType errorType,
  String errorMessage,
  int errorCode
) {

  public Error(
    ErrorType errorType,
    String errorMessage
  ) {
    this(errorType, errorMessage, -1);
  }

  public Error(
    ErrorType errorType
  ) {
    this(errorType, null, -1);
  }
}
