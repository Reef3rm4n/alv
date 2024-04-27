package io.alv.core.test.handlers;

import io.alv.core.Broadcast;
import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.Reply;
import io.alv.core.handler.MessageHandlerContext;
import io.alv.core.handler.MessageValidationContext;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import io.alv.core.test.messages.CounterIncremented;
import io.alv.core.test.messages.CounterNotFound;
import io.alv.core.test.messages.IncrementCounter;
import io.alv.core.test.model.Counter;

@Handler
@Reply({CounterIncremented.class, CounterNotFound.class})
@Broadcast({CounterIncremented.class})
public class IncrementCounterHandler implements MessageHandler<IncrementCounter> {

  @Override
  public void onValidation(MessageValidationContext<IncrementCounter> session) {
    if (session.message.failValidation()) {
      session.violations.add(new ConstraintViolation("Mocking validation failure", 1000));
    }
  }

  @Override
  public void onMessage(MessageHandlerContext<IncrementCounter> session) {
    session.get(session.message.id(), Counter.class)
      .ifPresentOrElse(c -> {
          final int newCount = c.current() + 1;
          session.put(session.message.id(), new Counter(newCount));
          session.reply(new CounterIncremented(session.message.id(), newCount));
        }
        , () -> session.reply(new CounterNotFound(session.message.id())));

  }

}
