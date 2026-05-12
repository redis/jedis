package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.FragmentedByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;


import redis.clients.jedis.exceptions.JedisBusyException;
import redis.clients.jedis.exceptions.JedisConnectionException;
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

//    Protocol.sendCommand(ros, Protocol.Command.GET, "SOMEKEY".getBytes(Protocol.CHARSET));
    Protocol.sendCommand(ros, new CommandArguments(Protocol.Command.GET).add("SOMEKEY"));
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

  @Test
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
      //ignore
    }
    assertThrows(IOException.class, ()-> ros.write((byte) '*'));
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

  // ==================== Verbatim String Tests (RESP3) ====================

  @Test
  public void verbatimStringReplyWithTxtFormat() {
    // RESP3 verbatim string: =<length>\r\n<format>:<data>\r\n
    // "txt:Hello World" has length 15 (4 bytes for "txt:" + 11 bytes for "Hello World")
    InputStream is = new ByteArrayInputStream("=15\r\ntxt:Hello World\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    // The "txt:" prefix should be stripped
    assertArrayEquals(SafeEncoder.encode("Hello World"), response);
  }

  @Test
  public void verbatimStringReplyWithMkdFormat() {
    // RESP3 verbatim string with markdown format
    // "mkd:# Header" has length 12 (4 bytes for "mkd:" + 8 bytes for "# Header")
    InputStream is = new ByteArrayInputStream("=12\r\nmkd:# Header\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    // The "mkd:" prefix should be stripped
    assertArrayEquals(SafeEncoder.encode("# Header"), response);
  }

  @Test
  public void verbatimStringReplyEmpty() {
    // Empty verbatim string: just the format prefix "txt:" with no data
    // Length is 4 (only the "txt:" prefix)
    InputStream is = new ByteArrayInputStream("=4\r\ntxt:\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    // The result should be empty after stripping "txt:"
    assertArrayEquals(new byte[0], response);
  }

  @Test
  public void verbatimStringReplyNull() {
    // Null verbatim string (length -1)
    InputStream is = new ByteArrayInputStream("=-1\r\n".getBytes());
    Object response = Protocol.read(new RedisInputStream(is));
    assertNull(response);
  }

  @Test
  public void verbatimStringReplyWithSpecialCharacters() {
    // Test verbatim string containing special characters like newlines
    String data = "Line1\nLine2\r\nLine3";
    String verbatimString = "txt:" + data;
    int length = verbatimString.length();
    String respString = "=" + length + "\r\n" + verbatimString + "\r\n";
    InputStream is = new ByteArrayInputStream(respString.getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(is));
    assertArrayEquals(SafeEncoder.encode(data), response);
  }

  @Test
  public void verbatimStringReplyMalformedTooShort() {
    // Verbatim string with length less than 4 (the minimum for the format prefix)
    // This should throw a JedisConnectionException
    InputStream is = new ByteArrayInputStream("=3\r\nabc\r\n".getBytes());
    assertThrows(JedisConnectionException.class, () -> {
      Protocol.read(new RedisInputStream(is));
    });
  }

  @Test
  public void fragmentedVerbatimStringReply() {
    // Test reading a verbatim string that arrives in fragments
    FragmentedByteArrayInputStream fis = new FragmentedByteArrayInputStream(
        "=34\r\ntxt:012345678901234567890123456789\r\n".getBytes());
    byte[] response = (byte[]) Protocol.read(new RedisInputStream(fis));
    // Should strip "txt:" and return the 30-character string
    assertArrayEquals(SafeEncoder.encode("012345678901234567890123456789"), response);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void verbatimStringInMultiBulkReply() {
    // Test verbatim string as part of a multi-bulk reply
    // *2\r\n$3\r\nfoo\r\n=11\r\ntxt:bar baz\r\n
    InputStream is = new ByteArrayInputStream(
        "*2\r\n$3\r\nfoo\r\n=11\r\ntxt:bar baz\r\n".getBytes());
    List<byte[]> response = (List<byte[]>) Protocol.read(new RedisInputStream(is));
    List<byte[]> expected = new ArrayList<byte[]>();
    expected.add(SafeEncoder.encode("foo"));
    expected.add(SafeEncoder.encode("bar baz")); // "txt:" should be stripped
    assertByteArrayListEquals(expected, response);
  }
}
