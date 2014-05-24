package redis.clients.jedis.tests;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;
import redis.clients.util.SocketChannelReader;
import redis.clients.util.SocketChannelWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ProtocolTest extends JedisTestBase {

    class SocketChannelWriterForTest extends SocketChannelWriter {
	public SocketChannelWriterForTest(SocketChannel channel, int size) {
	    super(channel, size);
	}

	public SocketChannelWriterForTest(SocketChannel channel) {
	    super(channel);
	}

	public byte[] readBuffer() {
	    ByteBuffer bufferForRead = buffer.duplicate();
	    bufferForRead.flip();

	    int remaining = bufferForRead.remaining();
	    byte[] message = new byte[remaining];
	    bufferForRead.get(message, 0, remaining);
	    return message;
	}
    }

    class SocketChannelReaderForTest extends SocketChannelReader {
	public SocketChannelReaderForTest(SocketChannel channel, int size) {
	    super(channel, size);
	}

	public SocketChannelReaderForTest(SocketChannel channel) {
	    super(channel);
	}

	public void loadBufferContent(byte[] content) {
	    buffer.clear();
	    buffer.put(content);
	    buffer.flip();
	    firstRead = false;
	}
    }

    private SocketChannelWriterForTest scw;
    private SocketChannelReaderForTest scr;
    private SocketChannel sc;

    @Before
    public void setUp() {
	sc = mock(SocketChannel.class);

	// we made very huge buffer that doesn't reach end during test
	scw = new SocketChannelWriterForTest(sc, 1024 * 10);
	scr = new SocketChannelReaderForTest(sc, 1024 * 10);
    }

    @Test
    public void buildACommand() throws IOException {
	Protocol.sendCommand(scw, Protocol.Command.GET,
		"SOMEKEY".getBytes(Protocol.CHARSET));

	String expectedCommand = "*2\r\n$3\r\nGET\r\n$7\r\nSOMEKEY\r\n";

	StringBuilder sb = new StringBuilder();
	for (byte b : scw.readBuffer())
	    sb.append((char) b);

	assertEquals(expectedCommand, sb.toString());
    }

    @Test
    public void buildALongCommand() throws IOException {
	int keyCount = 100;

	StringBuffer expectedCommandBuffer = new StringBuffer();

	expectedCommandBuffer.append("*" + (keyCount + 1) + "\r\n");
	expectedCommandBuffer.append("$3\r\nGET\r\n");

	byte[][] keys = new byte[keyCount][];
	for (int i = 0; i < keys.length; i++) {
	    String keyName = "SOMEKEY" + i;
	    keys[i] = keyName.getBytes(Protocol.CHARSET);
	    expectedCommandBuffer.append("$" + keyName.length() + "\r\n"
		    + keyName + "\r\n");
	}

	Protocol.sendCommand(scw, Protocol.Command.GET, keys);

	String expectedCommand = expectedCommandBuffer.toString();

	StringBuilder sb = new StringBuilder();
	for (byte b : scw.readBuffer())
	    sb.append((char) b);

	assertEquals(expectedCommand, sb.toString());
    }

    @Test
    public void bulkReply() {
	scr.loadBufferContent("$6\r\nfoobar\r\n".getBytes());
	byte[] response = (byte[]) Protocol.read(scr);
	assertArrayEquals(SafeEncoder.encode("foobar"), response);
    }

    @Test
    public void nullBulkReply() {
	scr.loadBufferContent("$-1\r\n".getBytes());
	String response = (String) Protocol.read(scr);
	assertEquals(null, response);
    }

    @Test
    public void singleLineReply() {
	scr.loadBufferContent("+OK\r\n".getBytes());
	byte[] response = (byte[]) Protocol.read(scr);
	assertArrayEquals(SafeEncoder.encode("OK"), response);
    }

    @Test
    public void integerReply() {
	scr.loadBufferContent(":123\r\n".getBytes());
	long response = (Long) Protocol.read(scr);
	assertEquals(123, response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void multiBulkReply() {
	scr.loadBufferContent("*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n"
		.getBytes());
	List<byte[]> response = (List<byte[]>) Protocol.read(scr);
	List<byte[]> expected = new ArrayList<byte[]>();
	expected.add(SafeEncoder.encode("foo"));
	expected.add(SafeEncoder.encode("bar"));
	expected.add(SafeEncoder.encode("Hello"));
	expected.add(SafeEncoder.encode("World"));

	assertEquals(expected, response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullMultiBulkReply() {
	scr.loadBufferContent("*-1\r\n".getBytes());
	List<String> response = (List<String>) Protocol.read(scr);
	assertNull(response);
    }
}