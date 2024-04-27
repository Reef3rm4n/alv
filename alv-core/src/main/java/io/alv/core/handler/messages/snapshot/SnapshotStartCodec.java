package io.alv.core.handler.messages.snapshot;

import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.SnapshotStartDecoder;
import io.alv.core.SnapshotStartEncoder;
import io.alv.core.handler.messages.encoding.DirectBufferCodec;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class SnapshotStartCodec implements DirectBufferCodec<SnapshotStart> {
  private final SnapshotStartDecoder decoder = new SnapshotStartDecoder();
  private final SnapshotStartEncoder encoder = new SnapshotStartEncoder();

  @Override
  public int encode(SnapshotStart message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(buffer, offset, headerEncoder);
    encoder.timestamp(message.timestamp());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }

  @Override
  public SnapshotStart decode(DirectBuffer directBuffer, int offset, int length, MessageHeaderDecoder headerDecoder) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoder);
    return new SnapshotStart(
      decoder.timestamp()
    );
  }
}
