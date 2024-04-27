package io.alv.core.handler.messages.storage;

import io.alv.core.handler.messages.objects.MessageEnvelope;

public record Long2ObjectEntry(
  long key,
  MessageEnvelope payload
) implements StorageEntry {
}
