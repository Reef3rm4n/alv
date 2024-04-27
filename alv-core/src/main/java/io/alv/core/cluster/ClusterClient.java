package io.alv.core.cluster;

import io.alv.core.handler.AeronMessageOffer;
import io.alv.core.handler.ClusterEgressListener;
import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.handles.BroadcastSubscription;
import io.alv.core.handler.messages.handles.MessageOffer;
import io.alv.core.handler.messages.input.InputMessage;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.codecs.AdminResponseCode;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import org.agrona.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static io.alv.core.handler.ClusterConfiguration.*;

public class ClusterClient implements Agent {
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);
  public static final Logger LOGGER = LoggerFactory.getLogger(ClusterClient.class);
  public static final SnowflakeIdGenerator SNOWFLAKE_GEN = new SnowflakeIdGenerator(SERVICE_ID);

  private static final long HEARTBEAT_INTERVAL = CLIENT_SESSION_TIMEOUT / 4;
  private final ClusterEgressListener clusterEgressListener;
  private long lastHeartbeatTime = Long.MIN_VALUE;
  private long logPosition = Long.MIN_VALUE;
  private AgentRunner agentRunner;
  private AeronCluster aeronCluster;
  private MediaDriver mediaDriver;


  public ClusterClient() {
    this.clusterEgressListener = new ClusterEgressListener();
  }

  public void connect() {
    final var mediaDriverContext = new MediaDriver.Context()
      .aeronDirectoryName(CLUSTER_DIR + "/client-media-driver")
      .threadingMode(ThreadingMode.SHARED)
      .dirDeleteOnStart(true)
      .errorHandler(throwable -> LOGGER.error("Error in media driver", throwable));
//    LOGGER.info("Media Driver Context: {}", mediaDriverContext);
    this.mediaDriver = MediaDriver.launch(mediaDriverContext);
    final var aeronContext = new AeronCluster.Context()
      .messageTimeoutNs(CLIENT_SESSION_TIMEOUT)
      .egressListener(clusterEgressListener)
      .egressChannel(EGRESS_CHANNEL)
      .ingressChannel(INGRESS_CHANNEL)
      .ingressEndpoints(ClusterConfig.ingressEndpoints(
          Arrays.stream(INGRESS_HOSTNAMES).toList(),
          CLUSTER_PORT,
          ClusterConfig.CLIENT_FACING_PORT_OFFSET
        )
      )
      .errorHandler(throwable -> LOGGER.error("Error in cluster connection", throwable))
      .aeronDirectoryName(mediaDriver.aeronDirectoryName());
//    LOGGER.info("Aeron Cluster Context: {}", aeronContext);
    this.aeronCluster = AeronCluster.connect(aeronContext);
    LOGGER.info("Connected to cluster leader, leaderId={} clientSessionId={}", aeronCluster.leaderMemberId(), aeronCluster.clusterSessionId());
    this.agentRunner = new AgentRunner(new SleepingMillisIdleStrategy(), error -> LOGGER.error("Agent dropped exception", error), null, this);
    AgentRunner.startOnThread(agentRunner);
  }

  public void disconnect() {
    decoderContext.remove();
    if (Objects.nonNull(aeronCluster)) {
      aeronCluster.close();
    }
    if (Objects.nonNull(mediaDriver)) {
      mediaDriver.close();
    }
    if (Objects.nonNull(agentRunner)) {
      agentRunner.close();
    }
  }

  public <M> void offer(MessageOffer<M> messageOffer) {
    final var snowflake = SNOWFLAKE_GEN.nextId();
    try {
      final var payload = MessageEnvelopeCodec.serialize(messageOffer.message());
      final var inputMessage = new InputMessage(snowflake, payload);
      final var buffer = decoderContext.get().encode(inputMessage);
      clusterEgressListener.register(snowflake, messageOffer);
      this.logPosition = AeronMessageOffer.offer(aeronCluster.context().idleStrategy(), aeronCluster, buffer, 0, buffer.capacity());
    } catch (Exception e) {
      LOGGER.error("Error sending message {}", messageOffer.message(), e);
      clusterEgressListener.unregister(snowflake);
      throw e;
    }
  }


  public void subscribe(BroadcastSubscription broadcastSubscription) {
    clusterEgressListener.register(broadcastSubscription);
  }

  public void unsubscribe(BroadcastSubscription broadcastSubscription) {
    clusterEgressListener.unregister(broadcastSubscription);
  }

  public long logPosition() {
    return logPosition;
  }


  @Override
  public int doWork() throws Exception {
    sendHeartBeat();
    pollEgress();
    return 0;
  }

  private void pollEgress() {
    if (null != aeronCluster && !aeronCluster.isClosed()) {
      aeronCluster.pollEgress();
    }
  }

  private void sendHeartBeat() {
    final long now = SystemEpochClock.INSTANCE.time();
    if (now >= (lastHeartbeatTime + HEARTBEAT_INTERVAL)) {
      lastHeartbeatTime = now;
      aeronCluster.sendKeepAlive();
    }
  }

  @Override
  public String roleName() {
    return "cluster-client";
  }

  public void takeSnapshot(Consumer<AdminResponseCode> consumer) {
    final long snowflake = SNOWFLAKE_GEN.nextId();
    clusterEgressListener.registerAdminMessageConsumer(snowflake, consumer);
    aeronCluster.sendAdminRequestToTakeASnapshot(snowflake);
  }
}
