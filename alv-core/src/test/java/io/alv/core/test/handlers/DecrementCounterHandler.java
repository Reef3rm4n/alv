package io.alv.core.test.handlers;

import io.alv.core.Broadcast;
import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.Reply;
import io.alv.core.handler.MessageHandlerContext;
import io.alv.core.handler.MessageValidationContext;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import io.alv.core.test.messages.CounterDecremented;
import io.alv.core.test.messages.CounterIncremented;
import io.alv.core.test.messages.CounterNotFound;
import io.alv.core.test.messages.DecrementCounter;
import io.alv.core.test.model.Counter;


@Handler
@Reply({CounterDecremented.class, CounterNotFound.class})
@Broadcast({CounterDecremented.class})
public class DecrementCounterHandler implements MessageHandler<DecrementCounter> {

  @Override
  public void onValidation(MessageValidationContext<DecrementCounter> session) {
    if (session.message.failValidation()) {
      session.violations.add(new ConstraintViolation("Mocking validation failure", 1000));
    }
  }

  @Override
  public void onMessage(MessageHandlerContext<DecrementCounter> session) {
    session.get(session.message.id(), Counter.class)
      .ifPresentOrElse(c -> {
          final int newCount = c.current() - 1;
          session.put(session.message.id(), new Counter(newCount));
          session.reply(new CounterIncremented(session.message.id(), 1));
          session.broadcast(new CounterIncremented(session.message.id(), 1));
        }
        , () -> {
          session.put(session.message.id(), new Counter(1));
          session.reply(new CounterNotFound(session.message.id()));
          session.broadcast(new CounterIncremented(session.message.id(), 1));
        });

  }

}
