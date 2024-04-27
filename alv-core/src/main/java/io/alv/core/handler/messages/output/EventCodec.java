package io.alv.core.handler.messages.output;

import io.alv.core.EventDecoder;
import io.alv.core.EventEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.handler.messages.objects.MessageEnvelope;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class EventCodec {
  private final EventEncoder encoder = new EventEncoder();
  private final EventDecoder decoder = new EventDecoder();

  public Event decode(DirectBuffer directBuffer, int offset, MessageHeaderDecoder headerDecoder, UnsafeBuffer payloadBuffer) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoder);
    final var timestamp = decoder.timestamp();
    final var snowflake = decoder.snowflake();
    final var payloadEncoding = decoder.payloadEncoding();
    final var payloadType = decoder.payloadType();
    final var length = decoder.payloadLength();
    decoder.getPayload(payloadBuffer, 0, length);
    return new Event(
      timestamp,
      snowflake,
      new MessageEnvelope(
        payloadEncoding,
        payloadType,
        length,
        payloadBuffer
      )
    );
  }

  public int encode(Event event, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(buffer, offset, headerEncoder)
      .snowflake(event.snowflake())
      .timestamp(event.timestamp())
      .payloadEncoding(event.payload().payloadEncoding())
      .payloadType(event.payload().payloadType())
      .putPayload(event.payload().payloadBuffer(), event.payload().offset(), event.payload().length());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }


}
