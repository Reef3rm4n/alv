package io.alv.core.handler.messages.snapshot;

import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.SnapshotEndDecoder;
import io.alv.core.SnapshotEndEncoder;
import io.alv.core.handler.messages.snapshot.SnapshotEnd;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class SnapshotEndCodec {
  private final SnapshotEndDecoder decoder = new SnapshotEndDecoder();
  private final SnapshotEndEncoder encoder = new SnapshotEndEncoder();

  public SnapshotEnd deserialize(DirectBuffer directBuffer, int offset, MessageHeaderDecoder headerDecoded) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoded);
    return new SnapshotEnd(
      decoder.timestamp(),
      decoder.fragmentCount()
    );
  }

  public int serialize(SnapshotEnd message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    encoder.wrapAndApplyHeader(buffer, offset, headerEncoder);
    encoder.timestamp(message.timestamp());
    encoder.fragmentCount(message.fragmentCount());
    return encoder.encodedLength() + headerEncoder.encodedLength();
  }
}
