package io.alv.core.test.handlers;

import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.handler.Context;
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
  public void onValidation(ValidationContext<DecrementCounter> session) {
    if (session.message.failValidation()) {
      session.unicast(new ConstraintViolation("Mocking constraint violation", 1000));
    }
  }

  @Override
  public void onMessage(Context<DecrementCounter> session) {
    session.state.get(session.message.id(), Counter.class)
      .ifPresentOrElse(
        c -> {
          final int newCount = c.current() - 1;
          session.state.put(session.message.id(), new Counter(newCount));
          session.unicast(new CounterIncremented(session.message.id(), 1));
          session.broadcast(new CounterIncremented(session.message.id(), 1));
        },
        () -> {
          session.state.put(session.message.id(), new Counter(1));
          session.unicast(new CounterNotFound(session.message.id()));
          session.broadcast(new CounterIncremented(session.message.id(), 1));
        }
      );
  }

}
