package redis.clients.jedis.tests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import redis.clients.jedis.Protocol;

public class ProtocolTest extends Assert {
    @Test
    public void buildACommand() {
	Protocol protocol = new Protocol();
	String command = protocol.buildCommand("GET", "SOMEKEY");

	String expectedCommand = "*2\r\n$3\r\nGET\r\n$7\r\nSOMEKEY\r\n";

	assertEquals(expectedCommand, command);
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