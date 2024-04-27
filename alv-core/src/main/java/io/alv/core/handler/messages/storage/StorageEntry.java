package io.alv.core.handler.messages.storage;

import io.alv.core.handler.messages.objects.MessageEnvelope;
import io.alv.core.handler.messages.storage.Int2ObjectEntry;
import io.alv.core.handler.messages.storage.Long2ObjectEntry;
import io.alv.core.handler.messages.storage.String2ObjectEntry;

public sealed interface StorageEntry permits String2ObjectEntry, Int2ObjectEntry, Long2ObjectEntry {

  MessageEnvelope payload();
}
