package redis.clients.util;

import redis.clients.jedis.exceptions.JedisConnectionException;

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
	buffer = ByteBuffer.allocateDirect(size);
    }

    public SocketChannelReader(SocketChannel channel) {
	this(channel, 8192);
    }

    public byte readByte() throws IOException {
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

	try {
	    while (true) {
		curr = readByte();
		if (curr == '\r') {
		    try {
			next = readByte();
		    } catch (IOException e) {
			sb.append((char) curr);
			break;
		    }

		    if (next == '\n') {
			break;
		    }

		    sb.append((char) curr);
		    sb.append((char) next);
		} else {
		    sb.append((char) curr);
		}
	    }
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}

	String reply = sb.toString();
	if (reply.length() == 0) {
	    throw new JedisConnectionException(
		    "It seems like server has closed the connection.");
	}
	return reply;
    }

    public int read(byte[] b, int off, int len) throws IOException {
	fillIfFirstRead();

	if (!buffer.hasRemaining()) {
	    try {
		fill();
	    } catch (IOException e) {
		return -1;
	    }
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

    private void fillIfFirstRead() throws IOException {
	if (firstRead) {
	    fill();

	    firstRead = false;
	}
    }

    private void fill() throws IOException {
	buffer.clear();
	if (channel.read(buffer) == -1) {
	    throw new IOException(
		    "It seems like server has closed the connection.");
	}
	buffer.flip();
    }

}
