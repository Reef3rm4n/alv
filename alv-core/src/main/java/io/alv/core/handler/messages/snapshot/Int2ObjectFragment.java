package io.alv.core.handler.messages.snapshot;

import org.agrona.MutableDirectBuffer;

public record Int2ObjectFragment(
  long timestamp,
  int fragmentNumber,
  int key,
  int offset,
  int length,
  MutableDirectBuffer payloadBuffer
) implements SnapshotMessage {
}
