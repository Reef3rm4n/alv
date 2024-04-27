package io.alv.core.handler;

import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class BufferSupplier {

  private BufferSupplier() {
  }

  public static final Integer ENVELOPE_SIZE = ClusterConfiguration.CONFIG.getOptionalValue("cluster.message.envelope.size", Integer.class)
    .orElse(1024) * 1024 + 512;

  public static final Integer PAYLOAD_SIZE = ClusterConfiguration.CONFIG.getOptionalValue("cluster.message.envelope.size", Integer.class)
    .orElse(1024) * 1024;

}
