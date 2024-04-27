package io.alv.core.handler.messages.snapshot;

import org.agrona.MutableDirectBuffer;

public record String2ObjectFragment(
  long timestamp,
  int fragmentNumber,
  String key,
  int offset,
  int length,
  MutableDirectBuffer payloadBuffer
) implements SnapshotMessage {
}
