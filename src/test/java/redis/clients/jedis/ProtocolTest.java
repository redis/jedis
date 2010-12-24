package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static redis.clients.jedis.Protocol.DEFAULT_CHARSET;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import org.testng.annotations.Test;


public class ProtocolTest {
    @Test
    public void buildACommand() throws IOException {
	PipedInputStream pis = new PipedInputStream();
	BufferedInputStream bis = new BufferedInputStream(pis);
	PipedOutputStream pos = new PipedOutputStream(pis);

	Protocol protocol = new Protocol();
	protocol.sendCommand(new RedisOutputStream(pos), Protocol.Command.GET,
		"SOMEKEY".getBytes(Protocol.CHARSET));

	pos.close();
	String expectedCommand = "*2\r\n$3\r\nGET\r\n$7\r\nSOMEKEY\r\n";

	int b;
	StringBuilder sb = new StringBuilder();
	while ((b = bis.read()) != -1) {
	    sb.append((char) b);
	}

	assertThat(sb.toString(), is(expectedCommand));
    }

    @Test
    public void bulkReply() {
	InputStream is = new ByteArrayInputStream("$6\r\nfoobar\r\n".getBytes());
	Protocol protocol = new Protocol();
	byte[] response = (byte[]) protocol.read(new RedisInputStream(is));
	assertThat(response, is(SafeEncoder.encode("foobar")));
    }

    @Test
    public void fragmentedBulkReply() {
	FragmentedByteArrayInputStream fis = new FragmentedByteArrayInputStream(
		"$30\r\n012345678901234567890123456789\r\n".getBytes());
	Protocol protocol = new Protocol();
	byte[] response = (byte[]) protocol.read(new RedisInputStream(fis));
	assertThat(response,
		is(SafeEncoder.encode("012345678901234567890123456789")));
    }

    @Test
    public void integerReply() {
	InputStream is = new ByteArrayInputStream(":123\r\n".getBytes());
	Protocol protocol = new Protocol();
	long response = (Long) protocol.read(new RedisInputStream(is));
	assertThat(response, is(123L));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void multiBulkReply() {
	InputStream is = new ByteArrayInputStream(
		"*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n"
			.getBytes(DEFAULT_CHARSET));
	Protocol protocol = new Protocol();
	List<byte[]> response = (List<byte[]>) protocol
		.read(new RedisInputStream(is));

	assertThat(
		response,
		contains(is("foo".getBytes(DEFAULT_CHARSET)),
			is("bar".getBytes(DEFAULT_CHARSET)),
			is("Hello".getBytes(DEFAULT_CHARSET)),
			is("World".getBytes(DEFAULT_CHARSET))));
    }

    @Test
    public void nullBulkReply() {
	InputStream is = new ByteArrayInputStream("$-1\r\n".getBytes());
	Protocol protocol = new Protocol();
	String response = (String) protocol.read(new RedisInputStream(is));
	assertThat(response, is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullMultiBulkReply() {
	InputStream is = new ByteArrayInputStream("*-1\r\n".getBytes());
	Protocol protocol = new Protocol();
	List<String> response = (List<String>) protocol
		.read(new RedisInputStream(is));
	assertThat(response, is(nullValue()));
    }

    @Test
    public void singleLineReply() {
	InputStream is = new ByteArrayInputStream("+OK\r\n".getBytes());
	Protocol protocol = new Protocol();
	byte[] response = (byte[]) protocol.read(new RedisInputStream(is));
	assertThat(response, is(SafeEncoder.encode("OK")));
    }
}