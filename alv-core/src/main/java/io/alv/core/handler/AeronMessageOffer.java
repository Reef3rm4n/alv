package io.alv.core.handler;

import io.alv.core.handler.exceptions.ConnectionClosedException;
import io.alv.core.handler.exceptions.DeserializationException;
import io.alv.core.handler.exceptions.DisconnectedException;
import io.alv.core.handler.exceptions.MaxLogPositionExceeded;
import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.service.ClientSession;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import static io.aeron.Publication.*;

public class AeronMessageOffer {

  private AeronMessageOffer() {
  }

  public static long offer(IdleStrategy idleStrategy, AeronCluster publication, DirectBuffer buffer, int offset, int length) {
    if (publication.isClosed()) {
      throw new ConnectionClosedException("Client session is closing");
    }
    idleStrategy.reset();
    long logPosition = -1;
    while (offer(publication, buffer, offset, length, logPosition)) {
      idleStrategy.idle();
    }
    return logPosition;
  }

  private static boolean offer(AeronCluster publication, DirectBuffer buffer, int offset, int length, long logPosition) {
    logPosition = publication.offer(buffer, offset, length);
    return shouldRetry(logPosition);
  }

  public static void offer(IdleStrategy idleStrategy, ClientSession publication, UnsafeBuffer buffer, int offset, int length) {
    if (publication.isClosing()) {
      throw new ConnectionClosedException("Client session is closing");
    }
    idleStrategy.reset();
    while (offer(publication, buffer, offset, length)) {
      idleStrategy.idle();
    }
  }

  private static boolean offer(ClientSession publication, UnsafeBuffer buffer, int offset, int length) {
    final long result = publication.offer(buffer, offset, length);
    return shouldRetry(result);
  }

  public static void offer(IdleStrategy idleStrategy, Publication publication, UnsafeBuffer buffer, int offset, int length) {
    if (publication.isClosed()) {
      throw new ConnectionClosedException("Client session is closed");
    }
    idleStrategy.reset();
    while (offer(publication, buffer, offset, length)) {
      idleStrategy.idle();
    }
  }

  private static boolean offer(Publication publication, UnsafeBuffer buffer, int offset, int length) {
    final long result = publication.offer(buffer, offset, length);
    return shouldRetry(result);
  }

  private static boolean shouldRetry(long result) {
    if (result > 0) {
      return false;
    } else if (result == BACK_PRESSURED || result == ADMIN_ACTION) {
      return true;
    } else {
      int resultInt = Math.toIntExact(result);
      if (resultInt == NOT_CONNECTED) {
        throw new DisconnectedException("Not connected to the cluster");
      } else if (resultInt == MAX_POSITION_EXCEEDED) {
        throw new MaxLogPositionExceeded("Max log position exceeded");
      } else if (resultInt == CLOSED) {
        throw new ConnectionClosedException("Connection closed");
      } else {
        throw new IllegalStateException("Unexpected result code {%s}".formatted(result));
      }
    }
  }

}
