/*
 * Copyright 2009-2010 MBTE Sweden AB. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

package redis.clients.jedis.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * This class assumes (to some degree) that we are reading a RESP stream. As such it assumes certain
 * conventions regarding CRLF line termination. It also assumes that if the Protocol layer requires
 * a byte that if that byte is not there it is a stream error.
 */
public class RedisInputStream extends FilterInputStream {

  private static final int INPUT_BUFFER_SIZE = Integer.parseInt(
      System.getProperty("jedis.bufferSize.input",
          System.getProperty("jedis.bufferSize", "8192")));

  protected final byte[] buf;

  protected int count, limit;

  public RedisInputStream(InputStream in, int size) {
    super(in);
    if (size <= 0) {
      throw new IllegalArgumentException("Buffer size <= 0");
    }
    buf = new byte[size];
  }

  public RedisInputStream(InputStream in) {
    this(in, INPUT_BUFFER_SIZE);
  }

  @Experimental
  public boolean peek(byte b) throws JedisConnectionException {
    ensureFill(); // in current design, at least one reply is expected. so ensureFillSafe() is not necessary.
    return buf[count] == b;
  }

  public byte readByte() throws JedisConnectionException {
    ensureFill();
    return buf[count++];
  }

  private void ensureCrLf() {
    final byte[] buf = this.buf;

    ensureFill();
    if (buf[count++] == '\r') {

      ensureFill();
      if (buf[count++] == '\n') {
        return;
      }
    }

    throw new JedisConnectionException("Unexpected character!");
  }

  public String readLine() {
    final StringBuilder sb = new StringBuilder();
    while (true) {
      ensureFill();

      byte b = buf[count++];
      if (b == '\r') {
        ensureFill(); // Must be one more byte

        byte c = buf[count++];
        if (c == '\n') {
          break;
        }
        sb.append((char) b);
        sb.append((char) c);
      } else {
        sb.append((char) b);
      }
    }

    final String reply = sb.toString();
    if (reply.isEmpty()) {
      throw new JedisConnectionException("It seems like server has closed the connection.");
    }

    return reply;
  }

  public byte[] readLineBytes() {

    /*
     * This operation should only require one fill. In that typical case we optimize allocation and
     * copy of the byte array. In the edge case where more than one fill is required then we take a
     * slower path and expand a byte array output stream as is necessary.
     */

    ensureFill();

    int pos = count;
    final byte[] buf = this.buf;
    while (true) {
      if (pos == limit) {
        return readLineBytesSlowly();
      }

      if (buf[pos++] == '\r') {
        if (pos == limit) {
          return readLineBytesSlowly();
        }

        if (buf[pos++] == '\n') {
          break;
        }
      }
    }

    final int N = (pos - count) - 2;
    final byte[] line = new byte[N];
    System.arraycopy(buf, count, line, 0, N);
    count = pos;
    return line;
  }

  /**
   * Slow path in case a line of bytes cannot be read in one #fill() operation. This is still faster
   * than creating the StringBuilder, String, then encoding as byte[] in Protocol, then decoding
   * back into a String.
   */
  private byte[] readLineBytesSlowly() {
    ByteArrayOutputStream bout = null;
    while (true) {
      ensureFill();

      byte b = buf[count++];
      if (b == '\r') {
        ensureFill(); // Must be one more byte

        byte c = buf[count++];
        if (c == '\n') {
          break;
        }

        if (bout == null) {
          bout = new ByteArrayOutputStream(16);
        }

        bout.write(b);
        bout.write(c);
      } else {
        if (bout == null) {
          bout = new ByteArrayOutputStream(16);
        }

        bout.write(b);
      }
    }

    return bout == null ? new byte[0] : bout.toByteArray();
  }

  public Object readNullCrLf() {
    ensureCrLf();
    return null;
  }

  public boolean readBooleanCrLf() {
    final byte[] buf = this.buf;

    ensureFill();
    final byte b = buf[count++];

    ensureCrLf();
    switch (b) {
      case 't':
        return true;
      case 'f':
        return false;
      default:
        throw new JedisConnectionException("Unexpected character!");
    }
  }

  public int readIntCrLf() {
    return (int) readLongCrLf();
  }

  public long readLongCrLf() {
    final byte[] buf = this.buf;

    ensureFill();

    final boolean isNeg = buf[count] == '-';
    if (isNeg) {
      ++count;
    }

    long value = 0;
    while (true) {
      ensureFill();

      final int b = buf[count++];
      if (b == '\r') {
        ensureFill();

        if (buf[count++] != '\n') {
          throw new JedisConnectionException("Unexpected character!");
        }

        break;
      } else {
        value = value * 10 + b - '0';
      }
    }

    return (isNeg ? -value : value);
  }

  public double readDoubleCrLf() {
    return DoublePrecision.parseFloatingPointNumber(readLine());
  }

  public BigInteger readBigIntegerCrLf() {
    return new BigInteger(readLine());
  }

  @Override
  public int read(byte[] b, int off, int len) throws JedisConnectionException {
    ensureFill();

    final int length = Math.min(limit - count, len);
    System.arraycopy(buf, count, b, off, length);
    count += length;
    return length;
  }

  /**
   * This method assumes there are required bytes to be read. If we cannot read anymore bytes an
   * exception is thrown to quickly ascertain that the stream was smaller than expected.
   */
  private void ensureFill() throws JedisConnectionException {
    if (count >= limit) {
      try {
        limit = in.read(buf);
        count = 0;
        if (limit == -1) {
          throw new JedisConnectionException("Unexpected end of stream.");
        }
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      }
    }
  }

  @Override
  public int available() throws IOException {
    int availableInBuf = limit - count;
    int availableInSocket = this.in.available();
    return (availableInBuf > availableInSocket) ? availableInBuf : availableInSocket;
  }

}
