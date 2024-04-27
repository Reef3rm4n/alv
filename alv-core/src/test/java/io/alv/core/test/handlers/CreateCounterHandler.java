package io.alv.core.test.handlers;

import io.alv.core.Broadcast;
import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.Reply;
import io.alv.core.handler.MessageHandlerContext;
import io.alv.core.handler.MessageValidationContext;
import io.alv.core.handler.messages.objects.ConstraintViolation;
import io.alv.core.test.messages.CounterAlreadyExists;
import io.alv.core.test.messages.CounterCreated;
import io.alv.core.test.messages.CreateCounter;
import io.alv.core.test.model.Counter;


@Handler
@Reply({CounterCreated.class, CounterAlreadyExists.class})
@Broadcast({CounterCreated.class})
public class CreateCounterHandler implements MessageHandler<CreateCounter> {

  @Override
  public void onValidation(MessageValidationContext<CreateCounter> session) {
    if (session.message.failValidation()) {
      session.violations.add(new ConstraintViolation("Mocking validation failure", 1000));
    }
  }

  @Override
  public void onMessage(MessageHandlerContext<CreateCounter> session) {
    session.get(session.message.id(), Counter.class)
      .ifPresentOrElse(c -> session.reply(new CounterAlreadyExists(session.message.id(), c.current()))
        ,
        () -> {
          session.put(session.message.id(), new Counter(0));
          session.reply(new CounterCreated(session.message.id()));
          session.broadcast(new CounterCreated(session.message.id()));
        }
      );

  }

}
