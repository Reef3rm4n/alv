package io.alv.core.test.handlers;

import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.handler.MessageContext;
import io.alv.core.handler.ReadOnlyMemoryStore;
import io.alv.core.handler.ReadWriteMemoryStore;
import io.alv.core.handler.ValidationContext;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import io.alv.core.test.messages.CounterIncremented;
import io.alv.core.test.messages.CounterNotFound;
import io.alv.core.test.messages.DecrementCounter;
import io.alv.core.test.model.Counter;


@Handler(
  unicast = {CounterNotFound.class},
  broadcast = {CounterIncremented.class}
)
public class DecrementCounterHandler implements MessageHandler<DecrementCounter> {

  @Override
  public void onValidation(ValidationContext<DecrementCounter> session, ReadOnlyMemoryStore memoryStore) {
    if (session.message.failValidation()) {
      session.unicast(new ConstraintViolation("Mocking constraint violation", 1000));
    }
  }

  @Override
  public void onMessage(MessageContext<DecrementCounter> session, ReadWriteMemoryStore memoryStore) {
    memoryStore.get(session.message.id(), Counter.class)
      .ifPresentOrElse(
        c -> {
          final int newCount = c.current() - 1;
          memoryStore.put(session.message.id(), new Counter(newCount));
          session.send(new CounterIncremented(session.message.id(), 1));
        },
        () -> session.send(new CounterNotFound(session.message.id()))
      );
  }

}
