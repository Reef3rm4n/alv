package io.alv.core.handler.messages.encoding.kyro;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class KyroCodec {
  private final Kryo kryo;

  public KyroCodec() {
    this.kryo = createKryo();
  }


  private static Kryo createKryo() {
    final var kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    kryo.setReferences(false);
    return kryo;
  }


  public void register(Class<?> clazz) {
    kryo.register(clazz);
  }

  public <T> T deserialize(DirectBuffer directBuffer, int offset, int length, Class<T> clazz) {
    return kryo.readObject(new Input(directBuffer.byteArray(), offset, length), clazz);
  }

  public <T> int serialize(T message, MutableDirectBuffer directBuffer, int offset) {
    final var output = new Output(directBuffer.byteArray(), directBuffer.capacity());
    output.setPosition(offset);
    kryo.writeObject(output, message);
    return output.position() - offset;
  }

}
