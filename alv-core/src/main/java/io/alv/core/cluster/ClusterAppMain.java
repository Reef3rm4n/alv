package io.alv.core.cluster;

import org.agrona.concurrent.ShutdownSignalBarrier;

public class ClusterAppMain {
  private static final ShutdownSignalBarrier SHUTDOWN_SIGNAL_BARRIER = new ShutdownSignalBarrier();

  public static void main(String[] args) {
    final ClusterApp app = new ClusterApp();
    app.start();
    SHUTDOWN_SIGNAL_BARRIER.await();
    app.close();
  }

}
