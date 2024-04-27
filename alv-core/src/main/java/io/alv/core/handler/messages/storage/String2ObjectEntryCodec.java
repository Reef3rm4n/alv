package io.alv.core.handler.messages.storage;

import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.String2ObjectEntryDecoder;
import io.alv.core.String2ObjectEntryEncoder;
import io.alv.core.handler.messages.objects.MessageEnvelope;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class String2ObjectEntryCodec {

  private final String2ObjectEntryDecoder decoder = new String2ObjectEntryDecoder();
  private final String2ObjectEntryEncoder encoder = new String2ObjectEntryEncoder();

  public int encode(String2ObjectEntry message, MutableDirectBuffer envelopeBuffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(envelopeBuffer, offset, headerEncoder);
    encoder.key(message.key());
    encoder.payloadEncoding(message.payload().payloadEncoding());
    encoder.payloadType(message.payload().payloadType());
    encoder.putPayload(message.payload().payloadBuffer(), 0, message.payload().length());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }

  public String2ObjectEntry decode(DirectBuffer envelopeBuffer, int offset, MessageHeaderDecoder headerDecoder, UnsafeBuffer payloadBuffer) {
    decoder.wrapAndApplyHeader(envelopeBuffer, offset, headerDecoder);
    final var key = decoder.key();
    final var payloadEncoding = decoder.payloadEncoding();
    final var payloadType = decoder.payloadType();
    final var payloadLength = decoder.payloadLength();
    decoder.getPayload(payloadBuffer, 0, payloadLength);
    return new String2ObjectEntry(
      key,
      new MessageEnvelope(
        payloadEncoding,
        payloadType,
        payloadLength,
        payloadBuffer
      )
    );
  }
}
