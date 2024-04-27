package io.alv.core.handler.messages.snapshot;

import org.agrona.MutableDirectBuffer;

public record Long2ObjectFragment(
  long timestamp,
  int fragmentNumber,
  long key,
  int offset,
  int length,
  MutableDirectBuffer payloadBuffer
) implements SnapshotMessage {
}
