package io.alv.core.cluster;

import io.alv.core.handler.ClusterConfiguration;
import io.alv.core.handler.messages.encoding.ClusterProtocolCodec;
import io.alv.core.handler.messages.handles.MessageOffer;
import io.alv.core.handler.messages.input.Input;
import io.alv.core.handler.messages.output.Ack;
import io.alv.core.handler.messages.output.ErrorMessage;
import io.alv.core.handler.messages.output.Event;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayParams;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static io.alv.core.handler.ClusterConfiguration.*;

public class ClusterArchiveReplay implements Agent {
  private static final ThreadLocal<ClusterProtocolCodec> decoderContext = ThreadLocal.withInitial(ClusterProtocolCodec::new);
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterArchiveReplay.class);
  private final Subscription replaySubscription;
  private final MediaDriver replayMediaDriver;
  private final Aeron replayAeronClient;
  private final AeronArchive replayArchiveClient;
  private final FragmentAssembler fragmentHandler;
  private final ClusterApp clusterApp;
  private final ClusterClient clusterClient;

  public ClusterArchiveReplay() {
    this.clusterApp = new ClusterApp();
    clusterApp.start();
    this.clusterClient = new ClusterClient();
    clusterClient.connect();
    this.replayMediaDriver = MediaDriver.launch(new MediaDriver.Context()
      .dirDeleteOnStart(true)
      .threadingMode(ThreadingMode.SHARED)
      .sharedIdleStrategy(new SleepingMillisIdleStrategy()));
    //connect an aeron client
    this.replayAeronClient = Aeron.connect(new Aeron.Context()
      .aeronDirectoryName(replayMediaDriver.aeronDirectoryName())
      .idleStrategy(new SleepingMillisIdleStrategy()));

    replayArchiveClient = AeronArchive.connect(
      new AeronArchive.Context()
        .controlRequestChannel(AERON_UDP_ENDPOINT + ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
        .recordingEventsChannel(AERON_UDP_ENDPOINT + ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
        .controlResponseChannel(AERON_UDP_ENDPOINT + ARCHIVE_HOSTNAME + ":" + ARCHIVE_PORT)
        .aeron(replayAeronClient)
    );
    LOGGER.info("Replay media driver context : {}", replayMediaDriver.context());
    LOGGER.info("Replay aeron client context : {}", replayAeronClient.context());
    LOGGER.info("Replay archive client context : {}", replayArchiveClient.context());
    this.replaySubscription = replayAeronClient.addSubscription("aeron:ipc", REPLAY_STREAM_ID);
    this.fragmentHandler = replayFragmentHandler();
    replayArchiveClient.startReplay(
      0,
      "aeron:ipc",
      ClusterConfiguration.REPLAY_STREAM_ID,
      new ReplayParams()
        .length(ClusterConfiguration.REPLAY_LENGTH)
        .position(ClusterConfiguration.REPLAY_START_POSITION)
    );

  }

  private FragmentAssembler replayFragmentHandler() {
    return new FragmentAssembler((buffer, offset, length, header) -> {
      final var input = decoderContext.get().decodeInput(buffer, offset, length);
      LOGGER.info("Replaying input: {}", input);
      clusterClient.offer(
        new MessageOffer<Input>(

        ) {
          @Override
          public Input message() {
            return input;
          }

          @Override
          public Consumer<Event> onEvent() {
            return event -> LOGGER.info("event: {}", event);
          }

          @Override
          public Consumer<Ack> onCompletion() {
            return ack -> LOGGER.info("ack: {}", ack);
          }

          @Override
          public Consumer<ErrorMessage> onError() {
            return errorMessage -> LOGGER.error("error: {}", errorMessage);
          }
        }
      );
    });
  }

  @Override
  public int doWork() {
    return replaySubscription.poll(fragmentHandler, 100);
  }

  @Override
  public String roleName() {
    return "archive-client-replay";
  }

  @Override
  public void onStart() {
    LOGGER.info("starting");
  }


  @Override
  public void onClose() {
    LOGGER.info("shutting down");
    replaySubscription.close();
    replayAeronClient.close();
    replayMediaDriver.close();
    replayArchiveClient.close();
    clusterClient.disconnect();
    clusterApp.close();
  }

}
