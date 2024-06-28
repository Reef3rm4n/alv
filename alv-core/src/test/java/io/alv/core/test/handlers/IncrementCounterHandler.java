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
import io.alv.core.test.messages.IncrementCounter;
import io.alv.core.test.model.Counter;

@Handler(broadcast = {CounterIncremented.class}, unicast = {CounterNotFound.class})
public class IncrementCounterHandler implements MessageHandler<IncrementCounter> {

  @Override
  public void onValidation(ValidationContext<IncrementCounter> session, ReadOnlyMemoryStore memoryStore) {
    if (session.message.failValidation()) {
      session.unicast(new ConstraintViolation("Mocking validation failure", 1000));
    }
  }

  @Override
  public void onMessage(MessageContext<IncrementCounter> session, ReadWriteMemoryStore memoryStore) {
    memoryStore.get(session.message.id(), Counter.class)
      .ifPresentOrElse(c -> {
          final int newCount = c.current() + 1;
          memoryStore.put(session.message.id(), new Counter(newCount));
          session.unicast(new CounterIncremented(session.message.id(), newCount));
        },
        () -> session.unicast(new CounterNotFound(session.message.id()))
      );

  }

}
