package io.alv.core.cluster;

import org.agrona.concurrent.ShutdownSignalBarrier;

public class GatewayAppMain {
  private static final ShutdownSignalBarrier SHUTDOWN_SIGNAL_BARRIER = new ShutdownSignalBarrier();

  public static void main(String[] args) {
    final GatewayApp app = new GatewayApp();
    app.start();
    SHUTDOWN_SIGNAL_BARRIER.await();
    app.stop();
  }

}
