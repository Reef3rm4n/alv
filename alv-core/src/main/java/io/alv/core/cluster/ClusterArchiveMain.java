package io.alv.core.cluster;

import org.agrona.concurrent.ShutdownSignalBarrier;

public class ClusterArchiveMain {
  private static final ShutdownSignalBarrier SHUTDOWN_SIGNAL_BARRIER = new ShutdownSignalBarrier();

  public static void main(String[] args) {
    final ClusterArchiveApp app = new ClusterArchiveApp();
    app.start();
    SHUTDOWN_SIGNAL_BARRIER.await();
    app.stop();
  }

}
