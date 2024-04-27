package io.alv.core.handler.messages.output;


import io.alv.core.AckDecoder;
import io.alv.core.ErrorMessageDecoder;
import io.alv.core.EventDecoder;
import io.alv.core.MessageHeaderDecoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class OutputEncoder {
  private final EventCodec eventCodec = new EventCodec();
  private final ErrorMessageCodec errorMessageCodec = new ErrorMessageCodec();
  private final AckCodec ackCodec = new AckCodec();

  public Output decode(DirectBuffer directBuffer, int offset, int length, MessageHeaderDecoder headerDecoder, UnsafeBuffer messagePayloadBuffer) {
    return switch (headerDecoder.templateId()) {
      case EventDecoder.TEMPLATE_ID -> eventCodec.decode(directBuffer, offset, headerDecoder, messagePayloadBuffer);
      case AckDecoder.TEMPLATE_ID -> ackCodec.decode(directBuffer, offset, headerDecoder);
      case ErrorMessageDecoder.TEMPLATE_ID -> errorMessageCodec.decode(directBuffer, offset, headerDecoder);
      default -> throw new IllegalArgumentException("Unknown message %s".formatted(headerDecoder.templateId()));
    };
  }

  public int encode(Output output, MutableDirectBuffer buffer, int offset, io.alv.core.MessageHeaderEncoder headerEncoder) {
    if (output instanceof Event event) {
      return eventCodec.encode(event, buffer, offset, headerEncoder);
    } else if (output instanceof ErrorMessage errorMessage) {
      return errorMessageCodec.encode(errorMessage, buffer, offset, headerEncoder);
    } else if (output instanceof Ack ack) {
      return ackCodec.encode(ack, buffer, offset, headerEncoder);
    }
    throw new IllegalArgumentException("Unsupported output errorMessage type");
  }

}
