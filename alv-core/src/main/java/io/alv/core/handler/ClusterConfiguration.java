package io.alv.core.handler;

import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClusterConfiguration {




  private ClusterConfiguration() {
  }
  public static final int REPLAY_STREAM_ID = 90;
  public static final String AERON_UDP = "aeron:udp";
  public static final String AERON_UDP_ENDPOINT = AERON_UDP + "?endpoint=";
  public static final SmallRyeConfig CONFIG = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);


  public static final long REPLAY_START_POSITION = CONFIG.getOptionalValue("replay.start.position", Long.class)
    .orElse(Long.MIN_VALUE);

  public static final long REPLAY_LENGTH = CONFIG.getOptionalValue("replay.length", Long.class)
    .orElse(Long.MIN_VALUE);


  public static final boolean DELETE_DIR_ON_START = ConfigProvider.getConfig()
    .getOptionalValue("cluster.deleteDirOnStart", Boolean.class)
    .orElse(false);
  public static final boolean ARCHIVE_DELETE_DIR_ON_START = ConfigProvider.getConfig()
    .getOptionalValue("archive.deleteDirOnStart", Boolean.class)
    .orElse(false);
  public static final int MEMBER_ID = CONFIG
    .getOptionalValue("cluster.memberId", Integer.class)
    .orElse(0);
  public static final int CLUSTER_PORT = CONFIG
    .getOptionalValue("cluster.port", Integer.class)
    .orElse(9000);

  public static final int ARCHIVE_PORT = CONFIG
    .getOptionalValue("archive.port", Integer.class)
    .orElse(9010);
  public static final String[] CLUSTER_HOSTNAMES = CONFIG
    .getOptionalValue("cluster.hostnames", String[].class)
    .orElse(List.of("localhost").toArray(String[]::new));
  public static final String[] INGRESS_HOSTNAMES = CONFIG
    .getOptionalValue("ingress.hostnames", String[].class)
    .orElse(List.of("localhost").toArray(String[]::new));
  public static final String HOSTNAME = CONFIG
    .getOptionalValue("cluster.hostname", String.class)
    .orElse("localhost");
  public static final Boolean DNS_DELAY = CONFIG
    .getOptionalValue("cluster.dns.delay", Boolean.class)
    .orElse(false);
  public static final String CLUSTER_DIR = CONFIG
    .getOptionalValue("cluster.dir", String.class)
    .orElse("/home/aeron");
  public static final String ARCHIVE_DIR = CONFIG
    .getOptionalValue("archive.dir", String.class)
    .orElse("/home/aeron/archive");
  public static final String ARCHIVE_HOSTNAME = CONFIG
    .getOptionalValue("archive.hostname", String.class)
    .orElse("localhost");
  public static final long ARCHIVE_BACKUP_INTERVAL = TimeUnit.SECONDS.toNanos(CONFIG
    .getOptionalValue("archive.interval", Long.class)
    .orElse(5L));
  public static final long CLIENT_SESSION_TIMEOUT = TimeUnit.SECONDS.toNanos(
    ClusterConfiguration.CONFIG
      .getOptionalValue("cluster.session.timeout", Integer.class)
      .orElse(10)
  );
  public static final long LEADER_HEARTBEAT_TIMEOUT = TimeUnit.SECONDS.toNanos(
    ClusterConfiguration.CONFIG
      .getOptionalValue("cluster.leader.heartbeat.timeout", Integer.class)
      .orElse(1)
  );

  public static final Integer SERVICE_MAJOR_VERSION = CONFIG.getOptionalValue("service.version.major", Integer.class).orElse(1);
  public static final Integer SERVICE_MINOR_VERSION = CONFIG.getOptionalValue("service.version.minor", Integer.class).orElse(0);
  public static final Integer SERVICE_PATCH_VERSION = CONFIG.getOptionalValue("service.version.patch", Integer.class).orElse(0);
  public static final String SERVICE_NAME = CONFIG.getOptionalValue("service.name", String.class).orElse("cluster-service");
  public static final int SERVICE_ID = CONFIG
    .getOptionalValue("service.id", Integer.class)
    .orElse(1);

  public static final String EGRESS_CHANNEL = CONFIG.getOptionalValue("cluster.egress.channel", String.class)
    .orElse(AERON_UDP_ENDPOINT + HOSTNAME + ":" + CLUSTER_PORT);

  public static final String INGRESS_CHANNEL = CONFIG.getOptionalValue("cluster.ingress.channel", String.class)
    .orElse("aeron:udp?term-length=64k");

}
