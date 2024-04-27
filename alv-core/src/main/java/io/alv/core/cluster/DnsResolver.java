package io.alv.core.cluster;

import io.alv.core.handler.ClusterConfiguration;
import org.agrona.concurrent.SystemEpochClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(DnsResolver.class);


  /**
   * Await DNS resolution of the given host. Under Kubernetes, this can take a while.
   *
   * @param host of the node to resolve
   */
  public static void awaitDnsResolution(final String host) {
    if (ClusterConfiguration.DNS_DELAY) {
      LOGGER.info("Waiting 5 seconds for DNS to be registered...");
      quietSleep(5000);
    }

    final long endTime = SystemEpochClock.INSTANCE.time() + 60000;
    java.security.Security.setProperty("networkaddress.cache.ttl", "0");

    boolean resolved = false;
    while (!resolved) {
      if (SystemEpochClock.INSTANCE.time() > endTime) {
        LOGGER.error("cannot resolve name {}, exiting", host);
        System.exit(-1);
      }
      try {
        final var ipAddress = InetAddress.getByName(host);
        LOGGER.info("resolved name {} to {}", host, ipAddress);
        resolved = true;
      } catch (final UnknownHostException e) {
        LOGGER.warn("cannot yet resolve name {}, retrying in 3 seconds", host);
        quietSleep(3000);
      }
    }
  }

  /**
   * Sleeps for the given number of milliseconds, ignoring any interrupts.
   *
   * @param millis the number of milliseconds to sleep.
   */
  private static void quietSleep(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException ex) {
      LOGGER.warn("Interrupted while sleeping");
    }
  }
}
