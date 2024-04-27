package io.alv.core.handler.exceptions;

public class ConnectionClosedException extends RuntimeException {
  public ConnectionClosedException(String message) {
    super(message);
  }
}
