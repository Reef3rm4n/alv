package io.alv.core.handler.messages.input;

import io.alv.core.*;
import io.alv.core.InputMessageDecoder;
import io.alv.core.InputMessageEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.handler.messages.objects.MessageEnvelope;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class MessageCodec {

  private final InputMessageEncoder encoder = new InputMessageEncoder();
  private final InputMessageDecoder decoder = new InputMessageDecoder();

  public InputMessage deserialize(DirectBuffer directBuffer, int offset, MessageHeaderDecoder headerDecoded, UnsafeBuffer payloadBuffer) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoded);
    final var snowflake = decoder.snowflake();
    final var payloadEncoding = decoder.payloadEncoding();
    final var payloadType = decoder.payloadType();
    final var payloadLength = decoder.payloadLength();
    decoder.getPayload(payloadBuffer, 0, payloadLength);
    return new InputMessage(
      snowflake,
      new MessageEnvelope(
        payloadEncoding,
        payloadType,
        offset,
        payloadLength,
        payloadBuffer
      )
    );
  }

  public int serialize(InputMessage inputMessage, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder encodedHeader) {
    encoder.wrapAndApplyHeader(buffer, offset, encodedHeader);
    encoder.snowflake(inputMessage.snowflake());
    encoder.payloadEncoding(inputMessage.messageEnvelope().payloadEncoding());
    encoder.payloadType(inputMessage.messageEnvelope().payloadType());
    encoder.putPayload(inputMessage.messageEnvelope().payloadBuffer(), inputMessage.messageEnvelope().offset(), inputMessage.messageEnvelope().length());
    return encodedHeader.encodedLength() + encoder.encodedLength();
  }
}
