# ALV (Aeron LMDB Vert.x)

ALV is a robust library designed to integrate Aeron Cluster, LMDB, and Vert.x, aimed at streamlining the development of Aeron Cluster applications. It enhances the implementation process by leveraging LMDB for state storage, and automates both cluster client and Gateway generation using Vert.x.

## Key Features

- **Auto-generation of Cluster Clients**: ALV automatically generates clients for each specified `MessageHandler`, which simplifies the client creation and management processes.
- **Multiple Message Encodings**: Supports various encoding schemes including KYRO, FURY, JSON, and SBE, offering developers the flexibility to select the most appropriate encoding for their needs.
- **Efficient Memory Management**: Utilizes static allocation with off-heap memory to optimize both memory usage and system performance.
- **Automatic Gateway Generation**: (Coming Soon) Facilitates the development of reactive JVM applications by automating Gateway creation in Vert.x, supporting protocols like WebSocket, GRPC, and HTTP.

## Example Usage

### Registering a Model for the State Machine

In ALV, use the `@Model` annotation to define a state machine model, which represents the application state and manages message interactions. The following example demonstrates how to register a `Counter` model:

```java
@Model(Encoding.FURY)
public record Counter(
  int current
) {
}
```

### Registering a Message Handler
The library provides a set of annotations (`@Handler`, `@Reply`, `@Broadcast`, etc.) that can be used to define message handlers. Here's an example of a message handler:

```java
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
```
### Using the Auto-generated Client

In this example, `IncrementCounterHandler` is a message handler that handles `IncrementCounter` messages. It can reply with `CounterIncremented` or `CounterNotFound` messages and broadcasts `CounterIncremented` messages.
Clients and gateway get automatically generated for this handler. Here's an example of how to use the auto-generated client:

  ```java
public abstract class CreateCounterOffer implements MessageOffer<CreateCounter> {
  private CreateCounter message;

  public CreateCounterOffer(CreateCounter message) {
    this.message = message;
  }

  public CreateCounter message() {
    return this.message;
  }

  public abstract void onCounterCreated(CounterCreated event);

  public abstract void onCounterAlreadyExists(CounterAlreadyExists event);

  public Consumer<Event> onEvent() {
    return event -> {
      Object message = io.alv.core.handler.messages.encoding.MessageEnvelopeCodec.deserialize(event.payload());
      switch (message.getClass().getSimpleName()) {
        case "CounterCreated":
          onCounterCreated((io.alv.core.test.messages.CounterCreated) message);
          break;
        case "CounterAlreadyExists":
          onCounterAlreadyExists((io.alv.core.test.messages.CounterAlreadyExists) message);
          break;
      }};
  }
}
```
Usage :
```java

private static String createCounter() {
  final var uuid = UUID.randomUUID().toString();
  CLUSTER_CLIENT.offer(new CreateCounterOffer(new CreateCounter(false, uuid)) {
    @Override
    public void onCounterCreated(CounterCreated event) {
      LOGGER.info("Counter created: {}", event);
    }

    @Override
    public void onCounterAlreadyExists(CounterAlreadyExists event) {
      LOGGER.info("Counter already exists: {}", event);
    }

    @Override
    public Consumer<Ack> onCompletion() {
      return ack -> LOGGER.info("Received Ack: {}", ack);
    }

    @Override
    public Consumer<ErrorMessage> onError() {
      return error -> LOGGER.error("Error: {}", error);
    }
  });
  return uuid;
}
```

## Getting Started
Soon ALV will be available on Maven Central. For now, you can clone the repository and build the project locally.


## Contributing

Contributions to ALV are welcome! Whether it's reporting issues, improving documentation, or contributing code, your contributions are appreciated.

## License

ALV is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for more details.
```
