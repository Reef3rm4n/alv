package io.alv.core.handler;

import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.output.Output;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import org.agrona.collections.Long2ObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ClientSessionManager {
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);

  public static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionManager.class);
  private final Long2ObjectHashMap<ClientSession> sessions = new Long2ObjectHashMap<>();
  private final Cluster cluster;

  public ClientSessionManager(Cluster cluster) {
    this.cluster = cluster;
  }


  public void addSession(ClientSession clientSession) {
    if (cluster.role() == Cluster.Role.LEADER) {
      sessions.put(clientSession.id(), clientSession);
    }
  }


  public void broadcast(Output output) {
    if (cluster.role() == Cluster.Role.LEADER) {
      sessions.values().forEach(session -> offer(session, output));
    }
  }

  public void unicast(long sessionId, Output output) {
    if (cluster.role() == Cluster.Role.LEADER) {
      final var session = sessions.get(sessionId);
      if (Objects.nonNull(session)) {
        offer(session, output);
      }
    }
  }


  public void close(ClientSession clientSession) {
    if (cluster.role() == Cluster.Role.LEADER) {
      if (sessions.containsKey(clientSession.id())) {
        sessions.remove(clientSession.id()).close();
      }
    }
    decoderContext.remove();
  }

  private void offer(ClientSession clientSession, Output output) {
    final var buffer = decoderContext.get().encode(output);
    AeronMessageOffer.offer(cluster.idleStrategy(), clientSession, buffer, 0, buffer.capacity());
  }

}
