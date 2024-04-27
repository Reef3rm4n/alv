package io.alv.core.handler.messages.output;


import io.alv.core.AckDecoder;
import io.alv.core.AckEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class AckCodec {

  private final AckEncoder encoder = new AckEncoder();
  private final AckDecoder decoder = new AckDecoder();

  public int encode(Ack message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(buffer, offset, headerEncoder);
    encoder.snowflake(message.snowflake());
    encoder.timestamp(message.timestamp());
    return encoder.encodedLength() + headerEncoder.encodedLength();
  }

  public Ack decode(DirectBuffer directBuffer, int offset, MessageHeaderDecoder headerDecoded) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoded);
    final var snowflake = decoder.snowflake();
    final var timestamp = decoder.timestamp();
    return new Ack(snowflake, timestamp);
  }
}
