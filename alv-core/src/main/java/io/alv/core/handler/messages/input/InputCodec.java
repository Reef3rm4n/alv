package io.alv.core.handler.messages.input;


import io.alv.core.InputMessageDecoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class InputCodec  {

  private final MessageCodec messageCodec = new MessageCodec();


  public Input decode(DirectBuffer directBuffer, int offset, int length, MessageHeaderDecoder headerDecoded, UnsafeBuffer messagePayloadBuffer) {
    return switch (headerDecoded.templateId()) {
      case InputMessageDecoder.TEMPLATE_ID ->
        messageCodec.deserialize(directBuffer, offset, headerDecoded, messagePayloadBuffer);
      default -> throw new IllegalArgumentException("Unsupported message type schemaId=%s templateId=%s sbeSchemaVersion=%s version=%s".formatted(headerDecoded.schemaId(), headerDecoded.templateId(), headerDecoded.sbeSchemaVersion(), headerDecoded.version()));
    };
  }
  public int encode(Input message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    if (message instanceof InputMessage msg) {
      return messageCodec.serialize(msg, buffer, offset, headerEncoder);
    }
    throw new IllegalArgumentException("Unsupported errorMessage type");
  }

}
