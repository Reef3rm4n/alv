package io.alv.core.handler.messages.snapshot;

import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.SnapshotStartDecoder;
import io.alv.core.SnapshotStartEncoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class SnapshotStartCodec {
  private final SnapshotStartDecoder decoder = new SnapshotStartDecoder();
  private final SnapshotStartEncoder encoder = new SnapshotStartEncoder();

  public SnapshotStart decode(DirectBuffer directBuffer, int offset, MessageHeaderDecoder headerDecoded) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoded);
    return new SnapshotStart(
      decoder.timestamp()
    );
  }

  public int encode(SnapshotStart message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(buffer, offset, headerEncoder);
    encoder.timestamp(message.timestamp());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }
}
