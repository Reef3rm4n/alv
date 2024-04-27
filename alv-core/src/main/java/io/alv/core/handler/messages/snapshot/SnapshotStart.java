package io.alv.core.handler.messages.snapshot;

public record SnapshotStart(
  long timestamp
) implements SnapshotMessage {
}
