package io.alv.core.handler.exceptions;

import io.alv.core.Encoding;

public class SerializationException extends RuntimeException {

  public SerializationException(String message) {
    super(message);
  }

  public SerializationException(Encoding messageEncoding, Class<?> messageClass, Exception e) {
    super(
      "Unable to serialize cluster message messageEncoder=[%s] messageClass=[%s]".formatted(messageEncoding.name(), messageClass.getName()),
      e
    );
  }
}
