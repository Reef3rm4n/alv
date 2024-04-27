package io.alv.core.handler.messages.snapshot;


import io.alv.core.Long2ObjectEntryFragmentDecoder;
import io.alv.core.Long2ObjectEntryFragmentEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class Long2ObjectFragmentCodec {

  private final Long2ObjectEntryFragmentDecoder decoder = new Long2ObjectEntryFragmentDecoder();
  private final Long2ObjectEntryFragmentEncoder encoder = new Long2ObjectEntryFragmentEncoder();

  public int serialize(Long2ObjectFragment message, MutableDirectBuffer envelopeBuffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(envelopeBuffer, offset, headerEncoder);
    encoder.timestamp(message.timestamp());
    encoder.fragmentNumber(message.fragmentNumber());
    encoder.key(message.key());
    encoder.putPayload(message.payloadBuffer(), 0, message.length());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }

  public Long2ObjectFragment deserialize(DirectBuffer envelopeBuffer, int offset, MessageHeaderDecoder headerDecoded, UnsafeBuffer payloadBuffer) {
    decoder.wrapAndApplyHeader(envelopeBuffer, offset, headerDecoded);
    final var timestamp = decoder.timestamp();
    final var fragmentNumber = decoder.fragmentNumber();
    final var key = decoder.key();
    final var payloadLength = decoder.payloadLength();
    decoder.getPayload(payloadBuffer, 0, payloadLength);
    return new Long2ObjectFragment(
      timestamp,
      fragmentNumber,
      key,
      0,
      payloadLength,
      payloadBuffer
    );
  }
}
