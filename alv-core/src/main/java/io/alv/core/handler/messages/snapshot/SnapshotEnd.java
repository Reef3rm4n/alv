package io.alv.core.handler.messages.snapshot;

public record SnapshotEnd(
  long timestamp,
  int fragmentCount
) implements SnapshotMessage {
}
