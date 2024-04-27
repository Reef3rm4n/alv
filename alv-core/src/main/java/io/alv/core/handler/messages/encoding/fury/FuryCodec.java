package io.alv.core.handler.messages.encoding.fury;

import io.fury.Fury;
import io.fury.memory.MemoryBuffer;
import io.fury.memory.MemoryUtils;
import io.fury.util.Platform;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuryCodec {
  public static final Logger LOGGER = LoggerFactory.getLogger(FuryCodec.class);
  private final Fury fury;

  public FuryCodec() {
    fury = Fury.builder()
      .withClassVersionCheck(false)
      .requireClassRegistration(true)
      .withCodegen(true)
      .build();
  }

  public void register(Class<?> clazz) {
    fury.register(clazz);
  }

  public  <T> T deserialize(DirectBuffer directBuffer, int offset, int length, Class<T> clazz) {
    final var memoryBuffer = MemoryUtils.buffer(Platform.getAddress(directBuffer.byteBuffer()),  length);
    return fury.deserializeJavaObject(memoryBuffer, clazz);
  }

  public <T> int serialize(T message, MutableDirectBuffer directBuffer, int offset) {
    final var memoryBuffer = MemoryUtils.buffer(Platform.getAddress(directBuffer.byteBuffer()),  directBuffer.capacity());
    fury.serializeJavaObject(memoryBuffer, message);
    return memoryBuffer.writerIndex() - offset;
  }

}
