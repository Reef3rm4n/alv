package io.alv.core.test;

import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.snapshot.String2ObjectFragment;
import io.alv.core.handler.messages.storage.String2ObjectEntry;
import io.alv.core.test.model.Counter;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class ClusterProtocolCodecTest {

  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);


  @Test
  void testSnapshotFragmentString2ObjectEntry() {
    final var fragmentBuffer = encode();
    final var string2ObjectFragment = (String2ObjectFragment) Assertions.assertDoesNotThrow(() -> decoderContext.get().decodeSnapshot(
        fragmentBuffer,
        0,
        fragmentBuffer.capacity()
      )
    );
    Assertions.assertNotNull(string2ObjectFragment);
    final var entry = (String2ObjectEntry) decoderContext.get().decodeEntry(
      string2ObjectFragment.payloadBuffer(),
      string2ObjectFragment.offset(),
      string2ObjectFragment.length()
    );
    final var result = MessageEnvelopeCodec.deserialize(entry.payload());
    Assertions.assertTrue(result instanceof Counter);
    Assertions.assertEquals(1, ((Counter) result).current());
  }

  private static UnsafeBuffer encode() {
    final var stringEntry = new String2ObjectEntry("key", MessageEnvelopeCodec.serialize(new Counter(1)));
    final var entryBuffer = decoderContext.get().encode(stringEntry);
    final var fragment = new String2ObjectFragment(
      Instant.now().toEpochMilli(),
      1,
      stringEntry.key(),
      0,
      entryBuffer.capacity(),
      entryBuffer
    );
    // since there's only one thread and we copied the buffer, we can reuse the buffer for the fragment
    return Assertions.assertDoesNotThrow(() -> decoderContext.get().encode(fragment));
  }

}
