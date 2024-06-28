package io.alv.core.test.handlers;

import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.handler.MessageContext;
import io.alv.core.handler.ReadOnlyMemoryStore;
import io.alv.core.handler.ReadWriteMemoryStore;
import io.alv.core.handler.ValidationContext;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import io.alv.core.test.messages.CounterAlreadyExists;
import io.alv.core.test.messages.CounterCreated;
import io.alv.core.test.messages.CreateCounter;
import io.alv.core.test.model.Counter;


@Handler(
  unicast = {CounterAlreadyExists.class},
  broadcast = {CounterCreated.class}
)
public class CreateCounterHandler implements MessageHandler<CreateCounter> {

  @Override
  public void onValidation(ValidationContext<CreateCounter> session, ReadOnlyMemoryStore memoryStore) {
    if (session.message.failValidation()) {
      session.unicast(new ConstraintViolation("Mocking validation failure", 1000));
    }
  }

  @Override
  public void onMessage(MessageContext<CreateCounter> session, ReadWriteMemoryStore memoryStore) {
    memoryStore.get(session.message.id(), Counter.class)
      .ifPresentOrElse(c -> session.unicast(new CounterAlreadyExists(session.message.id(), c.current())),
        () -> {
          memoryStore.put(session.message.id(), new Counter(0));
          session.unicast(new CounterCreated(session.message.id()));
          session.send(new CounterCreated(session.message.id()));
        }
      );
  }

}
