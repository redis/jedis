package redis.clients.jedis.tests;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import redis.clients.jedis.Protocol;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;

public class ProtocolTest extends Assert {
    @Test
    public void buildACommand() throws IOException {
	PipedInputStream pis = new PipedInputStream();
	BufferedInputStream bis = new BufferedInputStream(pis);
	PipedOutputStream pos = new PipedOutputStream(pis);

	Protocol protocol = new Protocol();
	protocol.sendCommand(new RedisOutputStream(pos), "GET", "SOMEKEY");

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
	String response = (String) protocol.read(new RedisInputStream(is));
	assertEquals("foobar", response);
    }

    @Test
    public void fragmentedBulkReply() {
    	FragmentedByteArrayInputStream fis = new FragmentedByteArrayInputStream("$30\r\n012345678901234567890123456789\r\n".getBytes());
    	Protocol protocol = new Protocol();
    	String response = (String) protocol.read(new RedisInputStream(fis));
    	assertEquals("012345678901234567890123456789", response);
//    	assertEquals(3, fis.getReadMethodCallCount());
    }

    
    @Test
    public void nullBulkReply() {
	InputStream is = new ByteArrayInputStream("$-1\r\n".getBytes());
	Protocol protocol = new Protocol();
	String response = (String) protocol.read(new RedisInputStream(is));
	assertEquals(null, response);
    }

    @Test
    public void singleLineReply() {
	InputStream is = new ByteArrayInputStream("+OK\r\n".getBytes());
	Protocol protocol = new Protocol();
	String response = (String) protocol.read(new RedisInputStream(is));
	assertEquals("OK", response);
    }

    @Test
    public void integerReply() {
	InputStream is = new ByteArrayInputStream(":123\r\n".getBytes());
	Protocol protocol = new Protocol();
	int response = (Integer) protocol.read(new RedisInputStream(is));
	assertEquals(123, response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void multiBulkReply() {
	InputStream is = new ByteArrayInputStream(
		"*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n"
			.getBytes());
	Protocol protocol = new Protocol();
	List<String> response = (List<String>) (List<?>) protocol
		.read(new RedisInputStream(is));
	List<String> expected = new ArrayList<String>();
	expected.add("foo");
	expected.add("bar");
	expected.add("Hello");
	expected.add("World");

	assertEquals(expected, response);

	is = new ByteArrayInputStream(
		"*4\r\n$3\r\nfoo\r\n+OK\r\n:1000\r\n*2\r\n$3\r\nfoo\r\n$3\r\nbar"
			.getBytes());
	protocol = new Protocol();
	List<Object> response2 = (List<Object>) protocol
		.read(new RedisInputStream(is));
	List<Object> expected2 = new ArrayList<Object>();
	expected2.add("foo");
	expected2.add("OK");
	expected2.add(1000);
	List<Object> sub = new ArrayList<Object>();
	sub.add("foo");
	sub.add("bar");
	expected2.add(sub);

	assertEquals(expected2, response2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullMultiBulkReply() {
	InputStream is = new ByteArrayInputStream("*-1\r\n".getBytes());
	Protocol protocol = new Protocol();
	List<String> response = (List<String>) protocol
		.read(new RedisInputStream(is));
	assertNull(response);
    }
}