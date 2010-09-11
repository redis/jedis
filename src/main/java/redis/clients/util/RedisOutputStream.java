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

    public void writeString(String str, CharsetEncoder encoder) throws IOException {
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

    private final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

    private final static byte [] DigitTens = {
	'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
	'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
	'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
	'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
	'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
	'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
	'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
	'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
	'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
	'9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
	} ;

    private final static byte [] DigitOnes = {
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	} ;

    private final static byte[] digits = {
	'0' , '1' , '2' , '3' , '4' , '5' ,
	'6' , '7' , '8' , '9' , 'a' , 'b' ,
	'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
	'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
	'o' , 'p' , 'q' , 'r' , 's' , 't' ,
	'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };

    public void writeInt(int value) throws IOException {
        if(value < 0) {
            write('-');
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
        char sign = 0;

        // Generate two digits per iteration
        while ( value >= 65536) {
            q = value / 100;
            r = value - ((q << 6) + (q << 5) + (q << 2));
            value = q;
            buf [--charPos] = DigitOnes[r];
            buf [--charPos] = DigitTens[r];
        }

        for (;;) {
            q = (value * 52429) >>> (16+3);
            r = value - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf [--charPos] = digits [r];
            value = q;
            if (value == 0) break;
        }
        count += size;
    }

    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
}