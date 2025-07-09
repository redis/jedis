package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.FragmentedByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

  @Test
  public void readPushEventsAreNotPropagatedAsReadOutputIfProcessed() {
    // Create a mock push listener
    final List<PushMessage> receivedMessages = new ArrayList<>();
    PushConsumer handler = pushContext -> {
        receivedMessages.add(pushContext.getMessage());
        pushContext.setProcessed(true);
    };

    // Create a stream with a push message followed by a regular response
    byte[] data = (">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nfoo\r\n+OK\r\n").getBytes();
    RedisInputStream is = new RedisInputStream(new ByteArrayInputStream(data));

    // Read the response, which should process the push message first
    Object response = Protocol.read(is, handler);

    // Verify the response
    assertArrayEquals(SafeEncoder.encode("OK"), (byte[]) response);

    // Verify the push message was received
    assertEquals(1, receivedMessages.size());
    PushMessage pushMessage = receivedMessages.get(0);
    assertEquals(2, pushMessage.getContent().size());
    assertEquals("invalidate", pushMessage.getType());
    assertArrayEquals(SafeEncoder.encode("invalidate"), (byte[]) pushMessage.getContent().get(0));
    
    // The second element should be a list with one element "foo"
    assertInstanceOf(List.class, pushMessage.getContent().get(1));
    List<?> keys = (List<?>) pushMessage.getContent().get(1);
    assertEquals(1, keys.size());
    assertArrayEquals(SafeEncoder.encode("foo"), (byte[]) keys.get(0));
  }

  @Test
  public void readMultiplePushEventsAreNotPropagatedAsReadOutputIfProcessed() {
    // Create a mock push listener
    final List<PushMessage> receivedMessages = new ArrayList<>();
    PushConsumer handler = pushContext -> { receivedMessages.add(pushContext.getMessage()); pushContext.setProcessed(true); };


    // Create a stream with multiple push messages followed by a regular response
    byte[] data = (
        ">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nfoo\r\n" +
        ">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nbar\r\n" +
        ">2\r\n$7\r\nmessage\r\n$5\r\nhello\r\n" +
        ":123\r\n"
    ).getBytes();
    RedisInputStream is = new RedisInputStream(new ByteArrayInputStream(data));

    // Read the response, which should process all push messages first
    Object response = Protocol.read(is, handler);

    // Verify the response
    assertEquals(123L, response);

    // Verify all push messages were received
    assertEquals(3, receivedMessages.size());
    
    // First push message (invalidate foo)
    PushMessage pushMessage1 = receivedMessages.get(0);
    assertArrayEquals(SafeEncoder.encode("invalidate"), (byte[]) pushMessage1.getContent().get(0));
    List<?> keys1 = (List<?>) pushMessage1.getContent().get(1);
    assertArrayEquals(SafeEncoder.encode("foo"), (byte[]) keys1.get(0));
    
    // Second push message (invalidate bar)
    PushMessage pushMessage2 = receivedMessages.get(1);
    assertArrayEquals(SafeEncoder.encode("invalidate"), (byte[]) pushMessage2.getContent().get(0));
    List<?> keys2 = (List<?>) pushMessage2.getContent().get(1);
    assertArrayEquals(SafeEncoder.encode("bar"), (byte[]) keys2.get(0));
    
    // Third push message (message hello)
    PushMessage pushMessage3 = receivedMessages.get(2);
    assertArrayEquals(SafeEncoder.encode("message"), (byte[]) pushMessage3.getContent().get(0));
    assertArrayEquals(SafeEncoder.encode("hello"), (byte[]) pushMessage3.getContent().get(1));
  }

  @Test
  public void readPushEventsArePropagateAsReadOutputIfNotProcessed() {
    // Create a mock push listener
    final List<PushMessage> receivedMessages = new ArrayList<>();
    PushConsumer handler = pushContext -> {
      receivedMessages.add(pushContext.getMessage());
      pushContext.setProcessed(false);
    };

    // Create a stream with a push message followed by a regular response
    byte[] data = (">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nfoo\r\n+OK\r\n").getBytes();
    RedisInputStream is = new RedisInputStream(new ByteArrayInputStream(data));

    // Read the response, which should return
    //    - invoke the push handler with the push message
    //    - propagate the push message as the read output since it was not processed
    Object pushMessage = Protocol.read(is, handler);

    // Verify the push message is propagated as the read output
    assertInstanceOf(ArrayList.class, pushMessage);
    assertArrayEquals(SafeEncoder.encode("invalidate"), (byte[]) ((ArrayList) pushMessage).get(0));

    // Verify the handler receives the push message
    assertEquals(1, receivedMessages.size());
    PushMessage push = receivedMessages.get(0);
    assertEquals(2, push.getContent().size());
    assertEquals("invalidate", push.getType());
    assertArrayEquals(SafeEncoder.encode("invalidate"), (byte[]) push.getContent().get(0));


    // Second read should return the command response itself
    Object commandResponse = Protocol.read(is, handler);

    // Verify the response
    assertArrayEquals(SafeEncoder.encode("OK"), (byte[]) commandResponse);
  }
}
