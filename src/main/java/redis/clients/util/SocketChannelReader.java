package redis.clients.util;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelReader {
  protected final ByteBuffer buffer;
  protected boolean firstRead = true;
  private final SocketChannel channel;

  public SocketChannelReader(SocketChannel channel, int size) {
    this.channel = channel;
    if (size <= 0) {
      throw new IllegalArgumentException("Buffer size <= 0");
    }
    buffer = ByteBuffer.allocate(size);
  }

  public SocketChannelReader(SocketChannel channel) {
    this(channel, 8192);
  }

  public byte readByte() throws JedisConnectionException {
    fillIfFirstRead();

    if (!buffer.hasRemaining()) {
      fill();
    }

    return buffer.get();
  }

  public String readLine() {
    byte curr;
    byte next;
    StringBuilder sb = new StringBuilder();

    while (true) {
      curr = readByte();
      if (curr == '\r') {
        next = readByte();
        if (next == '\n') {
          break;
        }

        sb.append((char) curr);
        sb.append((char) next);
      } else {
        sb.append((char) curr);
      }
    }

    String reply = sb.toString();
    if (reply.length() == 0) {
      throw new JedisConnectionException("It seems like server has closed the connection.");
    }
    return reply;
  }

  public byte[] readLineBytes() {
    byte curr;
    byte next;
    ByteArrayOutputStream bout = null;

    while (true) {
      curr = readByte();
      if (curr == '\r') {
        next = readByte();
        if (next == '\n') {
          break;
        }

        if (bout == null) {
          bout = new ByteArrayOutputStream(16);
        }

        bout.write(curr);
        bout.write(next);
      } else {
        if (bout == null) {
          bout = new ByteArrayOutputStream(16);
        }

        bout.write(curr);
      }
    }

    return bout == null ? new byte[0] : bout.toByteArray();
  }

  public int readIntCrLf() {
    return (int) readLongCrLf();
  }

  public long readLongCrLf() {
    byte b = readByte();
    final boolean isNeg = b == '-';

    if (isNeg) {
      b = readByte();
    }

    long value = b - '0';
    while (true) {
      b = readByte();
      if (b == '\r') {
        if (readByte() != '\n') {
          throw new JedisConnectionException("Unexpected character!");
        }

        break;
      } else {
        value = value * 10 + b - '0';
      }
    }

    return (isNeg ? -value : value);
  }

  public int read(byte[] b, int off, int len) throws JedisConnectionException {
    fillIfFirstRead();

    if (!buffer.hasRemaining()) {
      fill();
    }

    int length = 0;

    int index = off;
    int remainCopy = len;
    while (remainCopy > 0) {
      // off ~ off + len - 1
      int remaining = buffer.remaining();

      int putSize = Math.min(remaining, remainCopy);
      buffer.get(b, index, putSize);

      index += putSize;
      length += putSize;
      remainCopy -= putSize;

      if (!buffer.hasRemaining()) {
        fill();
      }
    }

    return length;
  }

  private void fillIfFirstRead() throws JedisConnectionException {
    if (firstRead) {
      fill();

      firstRead = false;
    }
  }

  private void fill() throws JedisConnectionException {
    buffer.clear();
    try {
      if (channel.read(buffer) == -1) {
        throw new JedisConnectionException("Unexpected end of stream.");
      }
      buffer.flip();
    } catch (IOException e) {
      throw new JedisConnectionException(e);
    }
  }

}
