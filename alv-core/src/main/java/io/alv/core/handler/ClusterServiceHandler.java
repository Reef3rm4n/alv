package io.alv.core.handler;

import io.alv.core.MessageHandler;
import io.alv.core.cluster.storage.Lmdb;
import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.input.InputMessage;
import io.alv.core.handler.messages.snapshot.*;
import io.alv.core.handler.messages.storage.Int2ObjectEntry;
import io.alv.core.handler.messages.storage.Long2ObjectEntry;
import io.alv.core.handler.messages.storage.String2ObjectEntry;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterServiceHandler implements ClusteredService {
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceHandler.class);
  private final Int2ObjectHashMap<MessageHandlingSession<?>> messageHandlers = new Int2ObjectHashMap<>();
  private final Lmdb lmdb;
  private ClientSessionManager clientSessionManager;
  private TimerManager timerManager;
  private Cluster cluster;
  private long logPosition;


  public ClusterServiceHandler() {
    this.lmdb = new Lmdb();
  }

  @Override
  public void onStart(Cluster cluster, Image snapshotImage) {
    LOGGER.info("Starting cluster-service snapshot={} logPosition={} role={} memberId={}",
      Objects.isNull(snapshotImage),
      cluster.logPosition(),
      cluster.role(),
      cluster.memberId()
    );
    this.cluster = cluster;
    this.clientSessionManager = new ClientSessionManager(cluster);
    this.timerManager = new TimerManager(cluster);
    loadHandlers();
    if (Objects.nonNull(snapshotImage)) {
      loadSnapshot(snapshotImage);
    }
  }

  private void loadHandlers() {
    ServiceLoader.load(Handlers.class).stream().map(ServiceLoader.Provider::get)
      .forEach(handlers -> handlers.handlers().forEach(messageHandler -> {
          final var messageType = TypeExtractor.getType(messageHandler);
          LOGGER.info("Registering message handler {}", messageType.getName());
          messageHandlers.put(
            messageType.getName().hashCode(),
            new MessageHandlingSession<>(
              timerManager,
              clientSessionManager,
              messageHandler,
              lmdb
            )
          );
        })
      );
  }

  private void loadSnapshot(Image snapshotImage) {
    final MutableBoolean isAllDataLoaded = new MutableBoolean(false);
    final FragmentAssembler fragmentHandler = new FragmentAssembler((buffer, offset, length, header) -> {
      final var message = decoderContext.get().decodeSnapshot(buffer, offset, length);
      if (message instanceof SnapshotStart snapshotStart) {
        LOGGER.info("Loading snapshot {}", snapshotStart);
      } else if (message instanceof String2ObjectFragment stateFragment) {
        lmdb.write((String2ObjectEntry) decoderContext.get().decodeEntry(stateFragment.payloadBuffer(), stateFragment.offset(), stateFragment.length()));
      } else if (message instanceof Long2ObjectFragment stateFragment) {
        lmdb.write((Long2ObjectEntry) decoderContext.get().decodeEntry(stateFragment.payloadBuffer(), stateFragment.offset(), stateFragment.length()));
      } else if (message instanceof Int2ObjectFragment stateFragment) {
        lmdb.write((Int2ObjectEntry) decoderContext.get().decodeEntry(stateFragment.payloadBuffer(), stateFragment.offset(), stateFragment.length()));
      } else if (message instanceof SnapshotEnd snapshotEnded) {
        LOGGER.info("Snapshot fully loaded {}", snapshotEnded);
        isAllDataLoaded.set(true);
      } else {
        throw new IllegalArgumentException("Unknown snapshot message");
      }
    }
    );
    while (!snapshotImage.isEndOfStream()) {
      final int fragmentsPolled = snapshotImage.poll(fragmentHandler, 100);
      if (isAllDataLoaded.value) {
        break;
      }
      cluster.idleStrategy().idle(fragmentsPolled);
    }
    if (!snapshotImage.isEndOfStream()) {
      throw new IllegalStateException("Cluster disagrees with application on snapshot");
    }
    if (!isAllDataLoaded.value) {
      throw new IllegalStateException("Application disagrees with cluster on snapshot");
    }
  }

  @Override
  public void onSessionOpen(ClientSession clientSession, long timestamp) {
    LOGGER.info("Session open  sessionId={} timestamp={}", clientSession.id(), timestamp);
    clientSessionManager.addSession(clientSession);

  }

  @Override
  public void onSessionClose(ClientSession clientSession, long timestamp, CloseReason closeReason) {
    LOGGER.info("Session closed sessionId={} timestamp={} reason={}", clientSession.id(), timestamp, closeReason.name());
    clientSessionManager.close(clientSession);
  }

  @Override
  public void onSessionMessage(
    ClientSession session,
    long timestamp,
    DirectBuffer buffer,
    int offset,
    int length,
    Header header
  ) {
    try {
      final var messageEnvelope = (InputMessage) decoderContext.get().decodeInput(buffer, offset, length);
      this.logPosition = header.position();
      messageHandlers.get(messageEnvelope.messageEnvelope().payloadType().hashCode())
        .onMessage(messageEnvelope, timestamp, session.id(), header.position());
    } catch (Exception e) {
      LOGGER.error("Error processing message sessionId={}", session.id(), e);
    }
  }

  @Override
  public void onTimerEvent(long correlationId, long timestamp) {
    final var messageEnvelope = timerManager.get(correlationId);
    messageHandlers.get(messageEnvelope.messageEnvelope().payloadType().hashCode())
      .onMessage(messageEnvelope, timestamp, 0, logPosition);
  }

  @Override
  public void onTakeSnapshot(ExclusivePublication exclusivePublication) {
    LOGGER.info("Taking snapshot");
    try {
      final var fragmentCounter = new AtomicInteger(0);
      final var snapshotPublisher = new SnapshotPublisher(
        cluster,
        fragmentCounter,
        exclusivePublication
      );
      snapshotPublisher.start();
      lmdb.snapshot(snapshotPublisher);
      snapshotPublisher.end();
      LOGGER.info("Snapshot taken fragments: {}", fragmentCounter.get());
    } catch (Exception e) {
      LOGGER.error("Error taking snapshot", e);
    }
  }

  @Override
  public void onRoleChange(Cluster.Role role) {
    LOGGER.info("Node changed role {}", role.name());
  }

  @Override
  public void onTerminate(Cluster cluster) {
    LOGGER.info("Cluster {} terminating logPosition={} memberId={} time={}", cluster.role(), cluster.logPosition(), cluster.memberId(), cluster.time());
    lmdb.close();
  }


}
