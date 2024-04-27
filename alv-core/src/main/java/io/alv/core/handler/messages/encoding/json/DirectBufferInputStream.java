package io.alv.core.handler.messages.encoding.json;

import org.agrona.DirectBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DirectBufferInputStream extends InputStream {
  private final int length;
  private final int offset;
  private final ByteBuffer byteBuffer;
  private int bytesRead = 0;

  public DirectBufferInputStream(DirectBuffer directBuffer, int offset, int length) {
    this.byteBuffer = directBuffer.byteBuffer();
    this.length = length;
    this.offset = offset;
  }

  @Override
  public int read() throws IOException {
    if (bytesRead >= length) {
      return -1; // End of stream
    }
    // Get the byte first, then increment bytesRead
    int value = byteBuffer.get(offset + bytesRead) & 0xFF; // Make sure to return unsigned byte
    bytesRead++;
    return value;
  }
}
