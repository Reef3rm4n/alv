package io.alv.core.handler.messages.output;


import io.alv.core.ErrorMessageDecoder;
import io.alv.core.ErrorMessageEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.handler.messages.objects.Error;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class ErrorMessageCodec {
  private final ErrorMessageDecoder decoder = new ErrorMessageDecoder();
  private final ErrorMessageEncoder encoder = new ErrorMessageEncoder();

  public int encode(ErrorMessage message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder encodedHeader) {
    encoder.wrapAndApplyHeader(buffer, offset, encodedHeader);
    encoder.timestamp(message.timestamp());
    encoder.snowflake(message.snowflake());
    encoder.errorType(message.error().errorType());
    encoder.message(message.error().errorMessage());
    encoder.code((byte) message.error().errorCode());
    return encodedHeader.encodedLength() + encoder.encodedLength();
  }

  public ErrorMessage decode(DirectBuffer directBuffer, int offset, MessageHeaderDecoder headerDecoded) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoded);
    return new ErrorMessage(
      decoder.timestamp(),
      decoder.snowflake(),
      new Error(
        decoder.errorType(),
        decoder.message(),
        decoder.code()
      )
    );
  }
}
