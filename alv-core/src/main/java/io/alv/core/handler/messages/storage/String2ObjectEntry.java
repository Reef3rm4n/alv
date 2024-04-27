package io.alv.core.handler.messages.storage;

import io.alv.core.handler.messages.objects.MessageEnvelope;

public record String2ObjectEntry(
  String key,
  MessageEnvelope payload
) implements StorageEntry {
}
