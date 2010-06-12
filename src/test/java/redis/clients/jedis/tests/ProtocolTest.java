package redis.clients.jedis.tests;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import redis.clients.jedis.Protocol;

public class ProtocolTest extends Assert {
    @Test
    public void buildACommand() throws IOException {
	PipedInputStream pis = new PipedInputStream();
	BufferedInputStream bis = new BufferedInputStream(pis);
	PipedOutputStream pos = new PipedOutputStream(pis);

	Protocol protocol = new Protocol();
	protocol.sendCommand(pos, "GET", "SOMEKEY");

	pos.close();
	String expectedCommand = "*2\r\n$3\r\nGET\r\n$7\r\nSOMEKEY\r\n";

	int b;
	StringBuilder sb = new StringBuilder();
	while ((b = bis.read()) != -1) {
	    sb.append((char) b);
	}

	assertEquals(expectedCommand, sb.toString());
    }

    @Test
    public void bulkReply() {
	InputStream is = new ByteArrayInputStream("$6\r\nfoobar\r\n".getBytes());
	Protocol protocol = new Protocol();
	String response = protocol.getBulkReply(is);
	assertEquals("foobar", response);
    }

    @Test
    public void nullBulkReply() {
	InputStream is = new ByteArrayInputStream("$-1\r\n".getBytes());
	Protocol protocol = new Protocol();
	String response = protocol.getBulkReply(is);
	assertEquals(null, response);
    }

    @Test
    public void singleLineReply() {
	InputStream is = new ByteArrayInputStream("+OK\r\n".getBytes());
	Protocol protocol = new Protocol();
	String response = protocol.getSingleLineReply(is);
	assertEquals("OK", response);
    }

    @Test
    public void integerReply() {
	InputStream is = new ByteArrayInputStream(":123\r\n".getBytes());
	Protocol protocol = new Protocol();
	int response = protocol.getIntegerReply(is);
	assertEquals(123, response);
    }

    @Test
    public void multiBulkReply() {
	InputStream is = new ByteArrayInputStream(
		"*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n"
			.getBytes());
	Protocol protocol = new Protocol();
	List<String> response = protocol.getMultiBulkReply(is);
	List<String> expected = new ArrayList<String>();
	expected.add("foo");
	expected.add("bar");
	expected.add("Hello");
	expected.add("World");

	assertEquals(expected, response);
    }
}