package redis.clients.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The class implements a buffered output stream without synchronization There are also special
 * operations like in-place string encoding. This stream fully ignore mark/reset and should not be
 * used outside Jedis
 */
public final class RedisOutputStream extends FilterOutputStream {
  protected final byte[] buf;

  protected int count;

  private final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999,
          999999999, Integer.MAX_VALUE };

  private final static byte[] DigitTens = { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1',
          '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2',
          '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4',
          '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6',
          '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8',
          '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', };

  private final static byte[] DigitOnes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
          '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
          '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
          '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4',
          '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
          '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };

  private final static byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
          'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
          't', 'u', 'v', 'w', 'x', 'y', 'z' };

  public RedisOutputStream(final OutputStream out) {
    this(out, 8192);
  }

  public RedisOutputStream(final OutputStream out, final int size) {
    super(out);
    if (size <= 0) {
      throw new IllegalArgumentException("Buffer size <= 0");
    }
    buf = new byte[size];
  }

  private void flushBuffer() throws IOException {
    if (count > 0) {
      out.write(buf, 0, count);
      count = 0;
    }
  }

  public void write(final byte b) throws IOException {
    if (count == buf.length) {
      flushBuffer();
    }
    buf[count++] = b;
  }

  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public void write(final byte[] b, final int off, final int len) throws IOException {
    if (len >= buf.length) {
      flushBuffer();
      out.write(b, off, len);
    } else {
      if (len >= buf.length - count) {
        flushBuffer();
      }

      System.arraycopy(b, off, buf, count, len);
      count += len;
    }
  }

  public void writeAsciiCrLf(final String in) throws IOException {
    final int size = in.length();

    for (int i = 0; i != size; ++i) {
      if (count == buf.length) {
        flushBuffer();
      }
      buf[count++] = (byte) in.charAt(i);
    }

    writeCrLf();
  }

  public static boolean isSurrogate(final char ch) {
    return ch >= Character.MIN_SURROGATE && ch <= Character.MAX_SURROGATE;
  }

  public static int utf8Length(final String str) {
    int strLen = str.length(), utfLen = 0;
    for (int i = 0; i != strLen; ++i) {
      char c = str.charAt(i);
      if (c < 0x80) {
        utfLen++;
      } else if (c < 0x800) {
        utfLen += 2;
      } else if (isSurrogate(c)) {
        i++;
        utfLen += 4;
      } else {
        utfLen += 3;
      }
    }
    return utfLen;
  }

  public void writeCrLf() throws IOException {
    if (2 >= buf.length - count) {
      flushBuffer();
    }

    buf[count++] = '\r';
    buf[count++] = '\n';
  }

  public void writeUtf8CrLf(final String str) throws IOException {
    int strLen = str.length();

    int i;
    for (i = 0; i < strLen; i++) {
      char c = str.charAt(i);
      if (!(c < 0x80)) break;
      if (count == buf.length) {
        flushBuffer();
      }
      buf[count++] = (byte) c;
    }

    for (; i < strLen; i++) {
      char c = str.charAt(i);
      if (c < 0x80) {
        if (count == buf.length) {
          flushBuffer();
        }
        buf[count++] = (byte) c;
      } else if (c < 0x800) {
        if (2 >= buf.length - count) {
          flushBuffer();
        }
        buf[count++] = (byte) (0xc0 | (c >> 6));
        buf[count++] = (byte) (0x80 | (c & 0x3f));
      } else if (isSurrogate(c)) {
        if (4 >= buf.length - count) {
          flushBuffer();
        }
        int uc = Character.toCodePoint(c, str.charAt(i++));
        buf[count++] = ((byte) (0xf0 | ((uc >> 18))));
        buf[count++] = ((byte) (0x80 | ((uc >> 12) & 0x3f)));
        buf[count++] = ((byte) (0x80 | ((uc >> 6) & 0x3f)));
        buf[count++] = ((byte) (0x80 | (uc & 0x3f)));
      } else {
        if (3 >= buf.length - count) {
          flushBuffer();
        }
        buf[count++] = ((byte) (0xe0 | ((c >> 12))));
        buf[count++] = ((byte) (0x80 | ((c >> 6) & 0x3f)));
        buf[count++] = ((byte) (0x80 | (c & 0x3f)));
      }
    }

    writeCrLf();
  }
  
  public void writeIntCrLf(int value) throws IOException {
    if (value < 0) {
      write((byte) '-');
      value = -value;
    }

    int size = 0;
    while (value > sizeTable[size])
      size++;

    size++;
    if (size >= buf.length - count) {
      flushBuffer();
    }

    int q, r;
    int charPos = count + size;

    while (value >= 65536) {
      q = value / 100;
      r = value - ((q << 6) + (q << 5) + (q << 2));
      value = q;
      buf[--charPos] = DigitOnes[r];
      buf[--charPos] = DigitTens[r];
    }

    for (;;) {
      q = (value * 52429) >>> (16 + 3);
      r = value - ((q << 3) + (q << 1));
      buf[--charPos] = digits[r];
      value = q;
      if (value == 0) break;
    }
    count += size;

    writeCrLf();
  }

  public void flush() throws IOException {
    flushBuffer();
    out.flush();
  }
}
