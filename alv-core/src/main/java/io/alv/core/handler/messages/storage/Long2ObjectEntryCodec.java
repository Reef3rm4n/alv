package io.alv.core.handler.messages.storage;

import io.alv.core.*;
import io.alv.core.Long2ObjectEntryDecoder;
import io.alv.core.Long2ObjectEntryEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.handler.messages.objects.MessageEnvelope;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class Long2ObjectEntryCodec {

  private final Long2ObjectEntryDecoder decoder = new Long2ObjectEntryDecoder();
  private final Long2ObjectEntryEncoder encoder = new Long2ObjectEntryEncoder();

  public int serialize(Long2ObjectEntry message, MutableDirectBuffer envelopeBuffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(envelopeBuffer, offset, headerEncoder);
    encoder.key(message.key());
    encoder.payloadEncoding(message.payload().payloadEncoding());
    encoder.payloadType(message.payload().payloadType());
    encoder.putPayload(message.payload().payloadBuffer(), 0, message.payload().length());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }

  public Long2ObjectEntry deserialize(DirectBuffer envelopeBuffer, int offset, MessageHeaderDecoder headerDecoded, UnsafeBuffer payloadBuffer) {
    decoder.wrapAndApplyHeader(envelopeBuffer, offset, headerDecoded);
    final var key = decoder.key();
    final var payloadEncoding = decoder.payloadEncoding();
    final var payloadType = decoder.payloadType();
    final var payloadLength = decoder.payloadLength();
    decoder.getPayload(payloadBuffer, 0, payloadLength);
    return new Long2ObjectEntry(
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
