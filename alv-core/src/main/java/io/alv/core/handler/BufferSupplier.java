package io.alv.core.handler;

public class BufferSupplier {

  private BufferSupplier() {
  }

  public static final Integer MAX_ENVELOPE_SIZE = ClusterConfiguration.CONFIG.getOptionalValue("cluster.message.envelope.size", Integer.class)
    .orElse(1024) * 1024 + 512;

  public static final Integer MAX_PAYLOAD_SIZE = ClusterConfiguration.CONFIG.getOptionalValue("cluster.message.envelope.size", Integer.class)
    .orElse(1024) * 1024;

}
