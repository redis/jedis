package redis.clients.jedis.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArrayListEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisBusyException;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public class ProtocolTest {
  @Test
  public void buildACommand() throws IOException {
    PipedInputStream pis = new PipedInputStream();
    BufferedInputStream bis = new BufferedInputStream(pis);
    PipedOutputStream pos = new PipedOutputStream(pis);
    RedisOutputStream ros = new RedisOutputStream(pos);

    Protocol.sendCommand(ros, Protocol.Command.GET, "SOMEKEY".getBytes(Protocol.CHARSET));
    ros.flush();
    pos.close();
    String expectedCommand = "*2\r\n$3\r\nGET\r\n$7\r\nSOMEKEY\r\n";

    int b;
    StringBuilder sb = new StringBuilder();
    while ((b = bis.read()) != -1) {
      sb.append((char) b);
    }

    assertEquals(expectedCommand, sb.toString());
  }

  @Test(expected = IOException.class)
  public void writeOverflow() throws IOException {
    RedisOutputStream ros = new RedisOutputStream(new OutputStream() {

      @Override
      public void write(int b) throws IOException {
        throw new IOException("thrown exception");

      }
    });

    ros.write(new byte[8191]);

    try {
      ros.write((byte) '*');
    } catch (IOException ioe) {
    }

    ros.write((byte) '*');

  }

  @Test
  public void bulkReply() {
    InputStream is = new ByteArrayInputStream("$6\r\nfoobar\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    assertArrayEquals(SafeEncoder.encode("foobar"), response);
  }

  @Test
  public void fragmentedBulkReply() {
    FragmentedByteArrayInputStream fis = new FragmentedByteArrayInputStream(
        "$30\r\n012345678901234567890123456789\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(fis));
    assertArrayEquals(SafeEncoder.encode("012345678901234567890123456789"), response);
  }

  @Test
  public void nullBulkReply() {
    InputStream is = new ByteArrayInputStream("$-1\r\n".getBytes());
    String response = (String) Protocol.read(new RedisInputStream(is));
    assertNull(response);
  }

  @Test
  public void singleLineReply() {
    InputStream is = new ByteArrayInputStream("+OK\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    assertArrayEquals(SafeEncoder.encode("OK"), response);
  }

  @Test
  public void integerReply() {
    InputStream is = new ByteArrayInputStream(":123\r\n".getBytes());
    long response = (Long) Protocol.read(new RedisInputStream(is));
    assertEquals(123, response);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void multiBulkReply() {
    InputStream is = new ByteArrayInputStream(
        "*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n".getBytes());
    List<byte[]> response = (List<byte[]>) Protocol.read(new RedisInputStream(is));
    List<byte[]> expected = new ArrayList<byte[]>();
    expected.add(SafeEncoder.encode("foo"));
    expected.add(SafeEncoder.encode("bar"));
    expected.add(SafeEncoder.encode("Hello"));
    expected.add(SafeEncoder.encode("World"));
    assertByteArrayListEquals(expected, response);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void nullMultiBulkReply() {
    InputStream is = new ByteArrayInputStream("*-1\r\n".getBytes());
    List<String> response = (List<String>) Protocol.read(new RedisInputStream(is));
    assertNull(response);
  }

  @Test
  public void busyReply() {
    final String busyMessage = "BUSY Redis is busy running a script.";
    final InputStream is = new ByteArrayInputStream(('-' + busyMessage + "\r\n").getBytes());
    try {
      Protocol.read(new RedisInputStream(is));
    } catch (final JedisBusyException e) {
      assertEquals(busyMessage, e.getMessage());
      return;
    }
    fail("Expected a JedisBusyException to be thrown.");
  }
}
