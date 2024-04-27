package io.alv.core.handler;

import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.snapshot.*;
import io.alv.core.handler.messages.storage.Int2ObjectEntry;
import io.alv.core.handler.messages.storage.Long2ObjectEntry;
import io.alv.core.handler.messages.storage.StorageEntry;
import io.alv.core.handler.messages.storage.String2ObjectEntry;
import io.aeron.ExclusivePublication;
import io.aeron.cluster.service.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class SnapshotPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotPublisher.class);
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);
  private final AtomicInteger fragmentCounter;
  private final ExclusivePublication snapshotPublication;
  private final Cluster cluster;

  public SnapshotPublisher(
    Cluster cluster,
    AtomicInteger fragmentCounter,
    ExclusivePublication publication
  ) {
    this.snapshotPublication = publication;
    this.cluster = cluster;
    this.fragmentCounter = fragmentCounter;
  }

  public void send(StorageEntry entry) {
    SnapshotMessage message;
    if (entry instanceof String2ObjectEntry string2ObjectEntry) {
      final var buffer = decoderContext.get().encode(string2ObjectEntry);
      message = new String2ObjectFragment(
        cluster.time(),
        fragmentCounter.getAndIncrement(),
        string2ObjectEntry.key(),
        0,
        buffer.capacity(),
        buffer
      );
    } else if (entry instanceof Long2ObjectEntry long2ObjectEntry) {
      final var buffer = decoderContext.get().encode(long2ObjectEntry);
      message = new Long2ObjectFragment(
        cluster.time(),
        fragmentCounter.getAndIncrement(),
        long2ObjectEntry.key(),
        0,
        buffer.capacity(),
        buffer
      );
    } else if (entry instanceof Int2ObjectEntry int2ObjectEntry) {
      final var buffer = decoderContext.get().encode(int2ObjectEntry);
      message = new Int2ObjectFragment(
        cluster.time(),
        fragmentCounter.getAndIncrement(),
        int2ObjectEntry.key(),
        0,
        buffer.capacity(),
        buffer
      );
    } else {
      throw new IllegalArgumentException("Unsupported entry type: " + entry.getClass());
    }
    final var buffer = decoderContext.get().encode(message);
    AeronMessageOffer.offer(cluster.idleStrategy(), snapshotPublication, buffer, 0, buffer.capacity());
  }

  void start() {
    final var startMessage = new SnapshotStart(cluster.time());
    final var buffer = decoderContext.get().encode(startMessage);
    AeronMessageOffer.offer(cluster.idleStrategy(), snapshotPublication, buffer, 0, buffer.capacity());
  }

  void end() {
    final var endMessage = new SnapshotEnd(cluster.time(), fragmentCounter.get());
    final var buffer = decoderContext.get().encode(endMessage);
    AeronMessageOffer.offer(cluster.idleStrategy(), snapshotPublication, buffer, 0, buffer.capacity());
  }

}
