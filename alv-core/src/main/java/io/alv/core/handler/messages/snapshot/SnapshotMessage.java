package io.alv.core.handler.messages.snapshot;


public sealed interface SnapshotMessage permits SnapshotEnd, Int2ObjectFragment, Long2ObjectFragment, String2ObjectFragment, SnapshotStart {
}
