package redis.clients.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * The class implements a buffered output stream without synchronization
 * There are also special operations like in-place string encoding
 */
public final class RedisOutputStream extends FilterOutputStream {
    protected final byte buf[];
    protected final ByteBuffer outByteBuffer;

    protected int count;

    public RedisOutputStream(OutputStream out) {
        this(out, 8192);
    }

    public RedisOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
        outByteBuffer = ByteBuffer.wrap(buf);
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            outByteBuffer.position(0);
            count = 0;
        }
    }

    public void write(int b) throws IOException {
        buf[count++] = (byte) b;
        if (count >= buf.length) {
            flushBuffer();
        }
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (len >= buf.length) {
            flushBuffer();
            out.write(b, off, len);
        }
        else {
            if (len >= buf.length - count) {
                flushBuffer();
            }

            System.arraycopy(b, off, buf, count, len);
            count += len;
        }
    }

    public void write(String str, CharsetEncoder encoder) throws IOException {
        final CharBuffer in = CharBuffer.wrap(str);
        if (in.remaining() == 0)
            return;

        outByteBuffer.position(count);

        encoder.reset();
        for (;;) {
            CoderResult cr;
            if (in.hasRemaining())
                cr = encoder.encode(in, outByteBuffer, true);
            else
                cr = encoder.flush(outByteBuffer);

            count = outByteBuffer.position();
            if(count == buf.length)
                flushBuffer();

            if (cr.isUnderflow()) {
                break;
            }
            if (cr.isOverflow()) {
                flushBuffer();
                continue;
            }
            cr.throwException();
        }
    }

    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
}
