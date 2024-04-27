package io.alv.core.handler.messages.snapshot;

import io.alv.core.*;
import io.alv.core.Int2ObjectEntryFragmentEncoder;
import io.alv.core.Long2ObjectEntryFragmentEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.SnapshotEndEncoder;
import io.alv.core.SnapshotStartEncoder;
import io.alv.core.String2ObjectEntryFragmentDecoder;
import io.alv.core.handler.messages.encoding.DirectBufferCodec;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class SnapshotCodec {
  private final SnapshotStartCodec snapshotStartCodec = new SnapshotStartCodec();
  private final String2ObjectFragmentCodec string2ObjectFragment = new String2ObjectFragmentCodec();
  private final Long2ObjectFragmentCodec long2ObjectFragmentCodec = new Long2ObjectFragmentCodec();
  private final Int2ObjectFragmentCodec int2ObjectFragmentCodec = new Int2ObjectFragmentCodec();
  private final SnapshotEndCodec snapshotEndCodec = new SnapshotEndCodec();

  public SnapshotMessage decode(DirectBuffer directBuffer, int offset, int length, MessageHeaderDecoder headerDecoder, UnsafeBuffer messagePayloadBuffer) {
    return switch (headerDecoder.templateId()) {
      case SnapshotStartEncoder.TEMPLATE_ID -> snapshotStartCodec.decode(directBuffer, offset, headerDecoder);
      case SnapshotEndEncoder.TEMPLATE_ID -> snapshotEndCodec.deserialize(directBuffer, offset, headerDecoder);
      case String2ObjectEntryFragmentDecoder.TEMPLATE_ID ->
        string2ObjectFragment.deserialize(directBuffer, offset, headerDecoder, messagePayloadBuffer);
      case Long2ObjectEntryFragmentEncoder.TEMPLATE_ID ->
        long2ObjectFragmentCodec.deserialize(directBuffer, offset, headerDecoder, messagePayloadBuffer);
      case Int2ObjectEntryFragmentEncoder.TEMPLATE_ID ->
        int2ObjectFragmentCodec.deserialize(directBuffer, offset, headerDecoder, messagePayloadBuffer);
      default -> throw new IllegalArgumentException("Unknown message %s".formatted(headerDecoder.templateId()));
    };
  }

  public int encode(SnapshotMessage message, MutableDirectBuffer buffer, int offset, MessageHeaderEncoder headerEncoder) {
    if (message instanceof SnapshotStart msg) {
      return snapshotStartCodec.encode(msg, buffer, offset, headerEncoder);
    } else if (message instanceof String2ObjectFragment fragment) {
      return string2ObjectFragment.serialize(fragment, buffer, offset, headerEncoder);
    } else if (message instanceof Long2ObjectFragment fragment) {
      return long2ObjectFragmentCodec.serialize(fragment, buffer, offset, headerEncoder);
    } else if (message instanceof Int2ObjectFragment fragment) {
      return int2ObjectFragmentCodec.serialize(fragment, buffer, offset, headerEncoder);
    } else if (message instanceof SnapshotEnd msg) {
      return snapshotEndCodec.serialize(msg, buffer, offset, headerEncoder);
    }
    throw new IllegalArgumentException("Unsupported snapshot message type");
  }

}
