package io.alv.core.handler.messages.storage;

import io.alv.core.handler.messages.objects.MessageEnvelope;

public record Int2ObjectEntry(
  int key,
  MessageEnvelope payload
) implements StorageEntry {
}
