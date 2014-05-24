package redis.clients.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelWriter {
    protected final ByteBuffer buffer;
    private final SocketChannel channel;

    public SocketChannelWriter(SocketChannel channel, int size) {
	this.channel = channel;
	if (size <= 0) {
	    throw new IllegalArgumentException("Buffer size <= 0");
	}
	buffer = ByteBuffer.allocateDirect(size);
    }

    public SocketChannelWriter(SocketChannel channel) {
	this(channel, 8192);
    }

    public void write(final byte b) throws IOException {
	if (buffer.hasRemaining()) {
	    buffer.put(b);
	} else {
	    flushBuffer();
	    buffer.put(b);
	}
    }

    public void write(final byte[] b) throws IOException {
	write(b, 0, b.length);
    }

    public void write(final byte b[], final int off, final int len)
	    throws IOException {
	if (!buffer.hasRemaining()) {
	    flushBuffer();
	}

	int index = off;
	int remainCopy = len;
	while (remainCopy > 0) {
	    // off ~ off + len - 1
	    int remaining = buffer.remaining();

	    int putSize = Math.min(remaining, remainCopy);
	    buffer.put(b, index, putSize);
	    index += putSize;
	    remainCopy -= putSize;

	    if (!buffer.hasRemaining()) {
		flushBuffer();
	    }
	}
    }

    public void writeCrLf() throws IOException {
	if (buffer.remaining() < 2) {
	    flushBuffer();
	}

	buffer.put((byte) '\r');
	buffer.put((byte) '\n');
    }

    private final static int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999,
	    9999999, 99999999, 999999999, Integer.MAX_VALUE };

    private final static byte[] DigitTens = { '0', '0', '0', '0', '0', '0',
	    '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1',
	    '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3',
	    '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4',
	    '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5',
	    '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7',
	    '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8',
	    '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9',
	    '9', '9', '9', };

    private final static byte[] DigitOnes = { '0', '1', '2', '3', '4', '5',
	    '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
	    '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1',
	    '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4',
	    '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7',
	    '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
	    '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3',
	    '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
	    '7', '8', '9', };

    private final static byte[] digits = { '0', '1', '2', '3', '4', '5', '6',
	    '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
	    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
	    'x', 'y', 'z' };

    public void writeIntCrLf(int value) throws IOException {
	if (value < 0) {
	    write((byte) '-');
	    value = -value;
	}

	int size = 0;
	while (value > sizeTable[size])
	    size++;

	size++;
	if (size >= buffer.remaining()) {
	    flushBuffer();
	}

	int q, r;
	int lastPos = buffer.position() + size;
	int charPos = lastPos;

	while (value >= 65536) {
	    q = value / 100;
	    r = value - ((q << 6) + (q << 5) + (q << 2));
	    value = q;
	    buffer.put(--charPos, DigitOnes[r]);
	    buffer.put(--charPos, DigitTens[r]);
	}

	for (;;) {
	    q = (value * 52429) >>> (16 + 3);
	    r = value - ((q << 3) + (q << 1));
	    buffer.put(--charPos, digits[r]);
	    value = q;
	    if (value == 0)
		break;
	}

	buffer.position(lastPos);
	writeCrLf();
    }

    public void flush() throws IOException {
	flushBuffer();
    }

    private void flushBuffer() throws IOException {
	// ready to write from buffer
	buffer.flip();

	// we have no data to write
	// position == limit == 0
	if (buffer.position() == buffer.limit()) {
	    buffer.clear();
	    return;
	}

	int sendSize = 0;
	while (sendSize == 0) {
	    // repeat sending if not sended
	    sendSize = channel.write(buffer);
	}
	buffer.compact();
    }

}
