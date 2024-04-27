package io.alv.core.test;

import io.alv.core.cluster.storage.Lmdb;
import io.alv.core.handler.ClientSessionManager;
import io.alv.core.handler.MessageHandlingSession;
import io.alv.core.handler.TimerManager;
import io.alv.core.handler.messages.encoding.MessageEnvelopeCodec;
import io.alv.core.handler.messages.input.InputMessage;
import io.alv.core.handler.messages.output.ErrorMessage;
import io.alv.core.handler.messages.output.Output;
import io.alv.core.test.messages.CreateCounter;
import io.alv.core.test.model.Counter;
import io.alv.core.test.handlers.CreateCounterHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

class MessageHandlerSessionTest {

  private static final Lmdb lmdb = new Lmdb();
  @Mock
  ClientSessionManager clientSessionManager = mock(ClientSessionManager.class);

  @Mock
  TimerManager timerManager = mock(TimerManager.class);

  @AfterAll
  static void stop() {
    lmdb.close();
  }

  private final MessageHandlingSession<CreateCounter> messageHandlingSession = new MessageHandlingSession<>(
    timerManager,
    clientSessionManager,
    new CreateCounterHandler(),
    lmdb
  );

  @AfterEach
  void cleanUp() {
    lmdb.cleanUp(Counter.class);
  }


  @Test
  void testCreateCounterHandler() {
    final var id = UUID.randomUUID().toString();
    final var input = new InputMessage(
      10L,
      MessageEnvelopeCodec.serialize(new CreateCounter(false, id))
    );
    messageHandlingSession.onMessage(input, 10L, 10L, 10L);
    try (final var txn = lmdb.txnRead()) {
      final var result = lmdb.get(txn, id, Counter.class).orElse(null);
      Assertions.assertNotNull(result);
    }
    verify(clientSessionManager, atLeastOnce()).send(anyLong(), any(Output.class));
    verify(clientSessionManager, atMostOnce()).broadcast(any(Output.class));
  }

  @Test
  void testCreateCounterFailed() {
    final var id = UUID.randomUUID().toString();
    final var input = new InputMessage(
      10L,
      MessageEnvelopeCodec.serialize(new CreateCounter(true, id))
    );
    messageHandlingSession.onMessage(input, 10L, 10L, 10L);
    try (final var txn = lmdb.txnRead()) {
      final var result = lmdb.get(txn, id, Counter.class).orElse(null);
      Assertions.assertNull(result);
    }
    verify(clientSessionManager, atLeastOnce()).send(anyLong(), any(ErrorMessage.class));
    verify(clientSessionManager, never()).broadcast(any(Output.class));
  }


}
