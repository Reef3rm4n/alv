package io.alv.core.test;

import io.alv.core.cluster.ClusterApp;
import io.alv.core.cluster.ClusterArchiveApp;
import io.alv.core.cluster.ClusterClient;
import io.alv.core.handler.messages.output.Ack;
import io.alv.core.handler.messages.output.ErrorMessage;
import io.alv.core.test.handlers.CreateCounterOffer;
import io.alv.core.test.handlers.IncrementCounterOffer;
import io.aeron.cluster.codecs.AdminResponseCode;
import io.alv.core.test.messages.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class TestClusterSingleNode {

  private final static ClusterApp CLUSTER_SERVICE_APP = new ClusterApp();
  private final static ClusterClient CLUSTER_CLIENT = new ClusterClient();
  private final static ClusterArchiveApp CLUSTER_BACKUP_APP = new ClusterArchiveApp();
  private static final Logger LOGGER = LoggerFactory.getLogger(TestClusterSingleNode.class);


  @AfterAll
  public static void stop_service() {
    CLUSTER_SERVICE_APP.close();
    CLUSTER_CLIENT.disconnect();
    CLUSTER_BACKUP_APP.stop();
  }

  @Test
  void test_simpleMessage() throws InterruptedException {
    CLUSTER_SERVICE_APP.start();
    CLUSTER_BACKUP_APP.start();
    CLUSTER_CLIENT.connect();
    CLUSTER_CLIENT.subscribe(event -> LOGGER.info("Broadcast: {}", event));
    IntStream.range(0 , 1).forEach(
      i -> {
          try {
              createAndIncrementCounter(1000);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
      }
    );
    final var id = createAndIncrementCounter(100);
    takeSnapshot();
    final var latch2 = new CountDownLatch(1);
    sendMessage(id, latch2, new AtomicInteger(), new AtomicInteger());
    Assertions.assertTrue(latch2.await(120, TimeUnit.SECONDS));
    Thread.sleep(5000);
  }

  private static String createAndIncrementCounter(int number) throws InterruptedException {
    final var id = createCounter();
    final AtomicInteger errorCounter = new AtomicInteger();
    final AtomicInteger ackCounter = new AtomicInteger();
    final var countDownLatch = new CountDownLatch(number);
    IntStream.range(0, number).forEach(i -> sendMessage(id, countDownLatch, errorCounter, ackCounter));
    Assertions.assertTrue(countDownLatch.await(30, TimeUnit.SECONDS));
    Assertions.assertEquals(0, errorCounter.get());
    Assertions.assertEquals(number, ackCounter.get());
    return id;
  }

  private void takeSnapshot() throws InterruptedException {
    final var latch = new CountDownLatch(1);
    CLUSTER_CLIENT.takeSnapshot(ack -> {
        Assertions.assertEquals(AdminResponseCode.OK, ack);
        latch.countDown();
      }
    );
    Assertions.assertTrue(latch.await(60, TimeUnit.SECONDS));
  }

  private static String createCounter() {
    final var uuid = UUID.randomUUID().toString();
    CLUSTER_CLIENT.offer(new CreateCounterOffer(new CreateCounter(false, uuid)) {
      @Override
      public void onCounterCreated(CounterCreated event) {
        LOGGER.info("Counter created: {}", event);
      }

      @Override
      public void onCounterAlreadyExists(CounterAlreadyExists event) {
        LOGGER.info("Counter already exists: {}", event);
      }

      @Override
      public Consumer<Ack> onCompletion() {
        return ack -> LOGGER.info("Received Ack: {}", ack);
      }

      @Override
      public Consumer<ErrorMessage> onError() {
        return error -> LOGGER.error("Error: {}", error);
      }
    });
    return uuid;
  }

  private static void sendMessage(String id, CountDownLatch latch, AtomicInteger errorCounter, AtomicInteger ackCounter) {

    CLUSTER_CLIENT.offer(new IncrementCounterOffer(
      new IncrementCounter(false, false, id)
    ) {
      @Override
      public void onCounterIncremented(CounterIncremented event) {
      }

      @Override
      public void onCounterNotFound(CounterNotFound event) {
      }

      @Override
      public Consumer<Ack> onCompletion() {
        return ack -> {
          LOGGER.info("Received Ack: {}", ack);
          ackCounter.getAndIncrement();
          latch.countDown();
        };
      }

      @Override
      public Consumer<ErrorMessage> onError() {
        return errorMessage -> {
          LOGGER.info("Error: {}", errorMessage);
          errorCounter.getAndIncrement();
          latch.countDown();
        };
      }
    });
  }

}
