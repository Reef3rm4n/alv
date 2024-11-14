# ALV (Aeron LMDB Vert.x)

ALV is a library designed to integrate Aeron Cluster, LMDB, and Vert.x, aimed at streamlining the development of Aeron and Aeron Cluster applications. It enhances the implementation process by leveraging LMDB(for workloads that wont fit available ram) or agrona data structures (for workloads that fit into ram) for state storage, and via codegen automates client generation and Gateway generation (Vert.x)

## Key Features
- **Automatic Snapshotting**: ALV automatically fragments the application state and returns the buffers to aeron for storage, ensuring that the application can recover from failures efficiently.
- **Cluster Archive**: ALV comes with a preconfigured aeron cluster archive that allows you to persist the message logs in disk and read it in different apps and recorver from crashes.
- **Auto-generation of Cluster Clients**: ALV automatically generates aeron clients for each specified `MessageHandler`, which simplifies the client creation and management processes.
- **Multiple Message Encodings**: Supports various encoding schemes including KYRO, FURY, JSON, and SBE, offering developers the flexibility to select the most appropriate encoding for their needs.
- **Efficient Memory Management**: Utilizes static allocation for deserialization and serialization with off-heap memory to optimize both memory usage and system performance.
- **Automatic Gateway Generation**: (Coming Soon) Facilitates the development of reactive JVM applications by automating Gateway creation in Vert.x, supporting protocols like WebSocket, GRPC, and HTTP.
- **Cloud ready Deployment charts**: (Coming Soon) To facilitate the deployment of both cluster and gateways in all the major cloud providers.

## Example Usage

### Registering a Model for the State Machine

In ALV, use the `@Model` annotation to define a state machine model. The following example demonstrates how to register a `Counter` model:

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
@Handler(
unicast={CounterIncremented.class, CounterNotFound.class},
broadcast={CounterIncremented.class})
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

### Implementing DirectBufferCodec for SBE Encoding and Decoding

In ALV, the `DirectBufferCodec` interface is used to implement encoding and decoding with SBE (Simple Binary Encoding).
This interface must be implemented for each message type that needs to be encoded or decoded.

Here's the `DirectBufferCodec` interface:

```java
public interface DirectBufferCodec<T> {
  int encode(T message, MutableDirectBuffer buffer, int offset);
  T decode(DirectBuffer directBuffer, int offset, int length);
}
```
Example :

```java
public class SnapshotStartCodec implements DirectBufferCodec<SnapshotStart> {
  private final SnapshotStartDecoder decoder = new SnapshotStartDecoder();
  private final SnapshotStartEncoder encoder = new SnapshotStartEncoder();
  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

  @Override
  public int encode(SnapshotStart message, MutableDirectBuffer buffer, int offset) {
    headerDecoder.wrap(buffer, offset);
    encoder.wrapAndApplyHeader(buffer, offset, headerEncoder);
    encoder.timestamp(message.timestamp());
    return headerEncoder.encodedLength() + encoder.encodedLength();
  }

  @Override
  public SnapshotStart decode(DirectBuffer directBuffer, int offset, int length) {
    decoder.wrapAndApplyHeader(directBuffer, offset, headerDecoder);
    return new SnapshotStart(
      decoder.timestamp()
    );
  }
}
```
## Getting Started
Soon ALV will be available on Maven Central. For now, you can clone the repository and build the project locally.


## Contributing

Contributions to ALV are welcome! Whether it's reporting issues, improving documentation, or contributing code, your contributions are appreciated.

## License

ALV is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for more details.
```
