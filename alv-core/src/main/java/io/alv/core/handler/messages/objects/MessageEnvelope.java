package io.alv.core.handler.messages.objects;

import io.alv.core.Encoding;
import org.agrona.MutableDirectBuffer;

public record MessageEnvelope(
  Encoding payloadEncoding,
  String payloadType,
  int offset,
  int length,
  MutableDirectBuffer payloadBuffer
) {


  public MessageEnvelope(Encoding payloadEncoding, String payloadType, int length, MutableDirectBuffer payloadBuffer) {
    this(payloadEncoding, payloadType, 0, length, payloadBuffer);
  }
}
