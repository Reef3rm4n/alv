package io.alv.core.handler.messages.encoding;

import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public interface DirectBufferCodec<T> {

  int encode(T message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder);

  T decode(DirectBuffer directBuffer, int offset, int length, MessageHeaderDecoder headerDecoder);

}
