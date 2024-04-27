package io.alv.core.handler.exceptions;

import io.alv.core.handler.messages.objects.MessageEnvelope;

public class DeserializationException extends RuntimeException {

  public DeserializationException(MessageEnvelope message, Exception e) {
    super(
      "Unable to deserialize payloadType=[%s] payloadEncoding=[%s]"
        .formatted(message.payloadType(),  message.payloadEncoding().name()),
      e
    );
  }
}
