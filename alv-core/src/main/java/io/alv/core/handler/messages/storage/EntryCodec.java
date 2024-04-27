package io.alv.core.handler.messages.storage;


import io.alv.core.Int2ObjectEntryEncoder;
import io.alv.core.Long2ObjectEntryEncoder;
import io.alv.core.String2ObjectEntryEncoder;
import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class EntryCodec {
  private final Int2ObjectEntryCodec int2ObjectEntryDecoder = new Int2ObjectEntryCodec();
  private final Long2ObjectEntryCodec long2ObjectEntryCodec = new Long2ObjectEntryCodec();
  private final String2ObjectEntryCodec string2ObjectEntryCodec = new String2ObjectEntryCodec();
  public int encode(StorageEntry entry, MutableDirectBuffer envelopeBuffer, int offset, MessageHeaderEncoder headerEncoder) {
    if (entry instanceof String2ObjectEntry string2ObjectEntry) {
      return string2ObjectEntryCodec.encode(string2ObjectEntry, envelopeBuffer, offset, headerEncoder);
    } else if (entry instanceof Int2ObjectEntry int2ObjectEntry) {
      return int2ObjectEntryDecoder.serialize(int2ObjectEntry, envelopeBuffer, offset, headerEncoder);
    } else if (entry instanceof Long2ObjectEntry long2ObjectEntry) {
      return long2ObjectEntryCodec.serialize(long2ObjectEntry, envelopeBuffer, offset, headerEncoder);
    }
    throw new IllegalArgumentException("Unsupported message type");
  }

  public StorageEntry decode(
    DirectBuffer directBuffer,
    int offset,
    int length,
    MessageHeaderDecoder headerDecoder,
    UnsafeBuffer entryUnsafeBuffer
  ) {
    return switch (headerDecoder.templateId()) {
      case Int2ObjectEntryEncoder.TEMPLATE_ID -> int2ObjectEntryDecoder.deserialize(
        directBuffer,
        offset,
        headerDecoder,
        entryUnsafeBuffer
      );
      case Long2ObjectEntryEncoder.TEMPLATE_ID -> long2ObjectEntryCodec.deserialize(
        directBuffer,
        offset,
        headerDecoder,
        entryUnsafeBuffer
      );
      case String2ObjectEntryEncoder.TEMPLATE_ID -> string2ObjectEntryCodec.decode(
        directBuffer,
        offset,
        headerDecoder,
        entryUnsafeBuffer
      );
      default -> throw new IllegalArgumentException("Unknown message %s".formatted(headerDecoder.templateId()));
    };
  }


}
