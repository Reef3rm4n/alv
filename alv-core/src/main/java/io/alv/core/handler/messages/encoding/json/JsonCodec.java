package io.alv.core.handler.messages.encoding.json;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL;

public class JsonCodec {


  public static final Logger LOGGER = LoggerFactory.getLogger(JsonCodec.class);
  public final ObjectMapper mapper;

  public JsonCodec() {
    mapper = new ObjectMapper()
      .configure(READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .registerModule(new JavaTimeModule())
      .registerModule(new Jdk8Module());
  }

  public <T> byte[] serialize(T outboundMessage) throws JsonProcessingException {
    return mapper.writeValueAsBytes(outboundMessage);
  }

  public <T> T deserialize(DirectBuffer directBuffer, int offset, int length, Class<T> clazz) throws IOException {
    return mapper.readValue(new DirectBufferInputStream(directBuffer, offset, length), clazz);
  }

  public <T> int serialize(T message, MutableDirectBuffer directBuffer, int offset) throws IOException {
    final var outputStream = new DirectBufferOutputStream(directBuffer, offset);
    mapper.writeValue(outputStream, message);
    return outputStream.length();
  }
}
