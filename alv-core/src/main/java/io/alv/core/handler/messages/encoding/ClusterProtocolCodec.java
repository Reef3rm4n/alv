package io.alv.core.handler.messages.encoding;


import io.alv.core.MessageHeaderDecoder;
import io.alv.core.MessageHeaderEncoder;
import io.alv.core.handler.BufferSupplier;
import io.alv.core.handler.messages.storage.EntryCodec;
import io.alv.core.handler.messages.input.InputCodec;
import io.alv.core.handler.messages.output.OutputEncoder;
import io.alv.core.handler.messages.snapshot.SnapshotCodec;
import io.alv.core.handler.messages.input.Input;
import io.alv.core.handler.messages.output.Output;
import io.alv.core.handler.messages.snapshot.SnapshotMessage;
import io.alv.core.handler.messages.storage.StorageEntry;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class ClusterProtocolCodec {
  private final ByteBuffer messagePayloadByteBuffer = ByteBuffer.allocateDirect(BufferSupplier.MAX_PAYLOAD_SIZE);
  private final UnsafeBuffer messagePayloadUnsafeBuffer = new UnsafeBuffer(messagePayloadByteBuffer);
  private final ByteBuffer envelopeByteBuffer = ByteBuffer.allocateDirect(BufferSupplier.MAX_ENVELOPE_SIZE);
  public final UnsafeBuffer envelopeUnsafeBuffer = new UnsafeBuffer(envelopeByteBuffer);
  private final ByteBuffer entryByteBuffer = ByteBuffer.allocateDirect(BufferSupplier.MAX_PAYLOAD_SIZE);
  private final UnsafeBuffer entryUnsafeBuffer = new UnsafeBuffer(entryByteBuffer);
  private final EntryCodec entryDecoder = new EntryCodec();
  private final OutputEncoder outputEncoder = new OutputEncoder();
  private final InputCodec inputCodec = new InputCodec();
  private final SnapshotCodec snapshotCodec = new SnapshotCodec();
  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

  public Input decodeInput(DirectBuffer buffer, int offset, int length) {
    headerDecoder.wrap(buffer, offset);
    return inputCodec.decode(buffer, offset, length, headerDecoder, messagePayloadUnsafeBuffer);
  }

  public UnsafeBuffer encode(Input entry) {
    envelopeUnsafeBuffer.wrap(envelopeByteBuffer);
    final int length = inputCodec.encode(entry, envelopeUnsafeBuffer, 0, headerEncoder);
    envelopeUnsafeBuffer.wrap(envelopeUnsafeBuffer, 0, length);
    return envelopeUnsafeBuffer;
  }

  public Output decodeOutput(DirectBuffer buffer, int offset, int length) {
    headerDecoder.wrap(buffer, offset);
    return outputEncoder.decode(buffer, offset, length, headerDecoder, messagePayloadUnsafeBuffer);
  }

  public UnsafeBuffer encode(Output entry) {
    envelopeUnsafeBuffer.wrap(envelopeByteBuffer);
    final int length = outputEncoder.encode(entry, envelopeUnsafeBuffer, 0, headerEncoder);
    envelopeUnsafeBuffer.wrap(envelopeUnsafeBuffer, 0, length);
    return envelopeUnsafeBuffer;
  }

  public StorageEntry decodeEntry(DirectBuffer buffer, int offset, int length) {
    headerDecoder.wrap(buffer, offset);
    entryUnsafeBuffer.wrap(entryByteBuffer);
    return entryDecoder.decode(buffer, offset, length, headerDecoder, entryUnsafeBuffer);
  }

  public UnsafeBuffer encode(StorageEntry entry) {
    entryUnsafeBuffer.wrap(entryByteBuffer);
    final int length = entryDecoder.encode(entry, entryUnsafeBuffer, 0, headerEncoder);
    entryUnsafeBuffer.wrap(entryUnsafeBuffer, 0, length);
    return entryUnsafeBuffer;
  }

  public SnapshotMessage decodeSnapshot(DirectBuffer buffer, int offset, int length) {
    headerDecoder.wrap(buffer, offset);
    return snapshotCodec.decode(buffer, offset, length, headerDecoder, messagePayloadUnsafeBuffer);
  }

  public UnsafeBuffer encode(SnapshotMessage snapshotMessage) {
    envelopeUnsafeBuffer.wrap(envelopeByteBuffer);
    final int length = snapshotCodec.encode(snapshotMessage, envelopeUnsafeBuffer, 0, headerEncoder);
    envelopeUnsafeBuffer.wrap(envelopeUnsafeBuffer, 0, length);
    return envelopeUnsafeBuffer;
  }


}
