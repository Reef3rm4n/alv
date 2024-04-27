package io.alv.core.handler.messages.encoding.json;

import org.agrona.MutableDirectBuffer;

import java.io.OutputStream;

public class DirectBufferOutputStream extends OutputStream {
  private final MutableDirectBuffer buffer;
  private final int offset;
  private int currentOffset;

  public DirectBufferOutputStream(MutableDirectBuffer directBuffer, int offset) {
    this.buffer = directBuffer;
    this.offset = offset;
    this.currentOffset = offset;
  }

  @Override
  public void write(int b) {
    buffer.putInt(currentOffset, (byte) b);
    currentOffset ++;
  }
  public int length() {
    return currentOffset - offset;
  }
}
