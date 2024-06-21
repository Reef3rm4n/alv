package io.alv.core.handler.messages.encoding;

import io.alv.core.Encoding;
import io.alv.core.handler.BufferSupplier;
import io.alv.core.handler.Messages;
import io.alv.core.handler.Models;
import io.alv.core.handler.TypeExtractor;
import io.alv.core.handler.messages.encoding.fury.FuryCodec;
import io.alv.core.handler.messages.encoding.json.JsonCodec;
import io.alv.core.handler.messages.encoding.kyro.KyroCodec;
import org.agrona.collections.Int2ObjectCache;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.ServiceLoader;

import static io.alv.core.handler.ClusterConfiguration.CONFIG;

public class MessageEnvelopeCodecContext {
  final Int2ObjectHashMap<DirectBufferCodec<?>> sbeEncoders = new Int2ObjectHashMap<>();
  final Int2ObjectHashMap<Class<?>> messageTypes = new Int2ObjectHashMap<>();
  final Int2ObjectHashMap<Encoding> messageEncoding = new Int2ObjectHashMap<>();
  final FuryCodec fury;
  final KyroCodec kyro;
  final JsonCodec json;

  final Int2ObjectCache<Class<?>> messageTypesCache = new Int2ObjectCache<>(
    32,
    128,
    e -> {
    }
  );
  final Int2ObjectCache<Encoding> encodingCache = new Int2ObjectCache<>(
    32,
    128,
    evicted -> {
    });
  final Int2ObjectCache<DirectBufferCodec<?>> sbeEncoderCache = new Int2ObjectCache<>(
    32,
    128,
    evicted -> {
    }
  );

  private final ByteBuffer messageEnvelopeByteBuffer = ByteBuffer.allocateDirect(BufferSupplier.MAX_ENVELOPE_SIZE);

  private final UnsafeBuffer messageEnvelopeBuffer = new UnsafeBuffer(messageEnvelopeByteBuffer);


  public UnsafeBuffer messageEnvelopeBuffer() {
    messageEnvelopeBuffer.wrap(messageEnvelopeByteBuffer);
    return messageEnvelopeBuffer;
  }

  public MessageEnvelopeCodecContext() {
    fury = CONFIG.getOptionalValue("codec.fury", Boolean.class)
      .orElse(true) ? new FuryCodec() : null;
    kyro = CONFIG.getOptionalValue("codec.kryo", Boolean.class)
      .orElse(false) ? new KyroCodec() : null;
    json = CONFIG.getOptionalValue("codec.json", Boolean.class)
      .orElse(false) ? new JsonCodec() : null;
    loadModels();
    loadMessages();
    loadSbeEncoders();
  }

  private void loadSbeEncoders() {
    ServiceLoader.load(DirectBufferCodec.class).stream()
      .map(ServiceLoader.Provider::get)
      .forEach(this::addSbeEncoder);
  }

  public void addSbeEncoder(DirectBufferCodec<?> directBufferEncoder) {
    final var messageClass = TypeExtractor.getType(directBufferEncoder);
    messageTypes.put(messageClass.getName().hashCode(), messageClass);
    messageTypesCache.put(messageClass.getName().hashCode(), messageClass);
    sbeEncoders.put(messageClass.getName().hashCode(), directBufferEncoder);
    encodingCache.put(messageClass.getName().hashCode(), Encoding.SBE);
    messageEncoding.put(messageClass.getName().hashCode(), Encoding.SBE);
  }

  private void loadMessages() {
    ServiceLoader.load(Messages.class).stream()
      .map(ServiceLoader.Provider::get)
      .forEach(registerModels -> registerModels.messages()
        .forEach((clazz, encoding) -> {
            messageTypes.put(clazz.getName().hashCode(), clazz);
            messageEncoding.put(clazz.getName().hashCode(), encoding);
            switch (encoding) {
              case FURY -> fury.register(clazz);
              case KYRO -> kyro.register(clazz);
              case JSON, SBE, NULL_VAL -> {
              }
              default -> throw new IllegalArgumentException("Unsupported encoding type: " + encoding);
            }
          }
        )
      );
  }

  private void loadModels() {
    ServiceLoader.load(Models.class).stream()
      .map(ServiceLoader.Provider::get)
      .forEach(models -> models.models()
        .forEach((clazz, encoding) -> {
            messageTypes.put(clazz.getName().hashCode(), clazz);
            messageEncoding.put(clazz.getName().hashCode(), encoding);
            switch (encoding) {
              case FURY -> fury.register(clazz);
              case KYRO -> kyro.register(clazz);
              case JSON, SBE, NULL_VAL -> {
              }
              default -> throw new IllegalArgumentException("Unsupported encoding type: " + encoding);
            }
          }
        )
      );
  }
}
