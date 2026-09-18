package redis.clients.jedis.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisPubSub;

/** Test utilities for asserting on Pub/Sub notifications. */
public final class PubSubHelpers {

  public static final long DEFAULT_AWAIT_MILLIS = 5_000L;

  private static final long SUBSCRIBED_AWAIT_SECONDS = 5L;

  private PubSubHelpers() {
  }

  /** Asserts that the subscriber became active within 5 seconds. */
  public static void awaitSubscribed(CountDownLatch subscribed) throws InterruptedException {
    assertThat("subscriber did not become active",
      subscribed.await(SUBSCRIBED_AWAIT_SECONDS, TimeUnit.SECONDS), equalTo(true));
  }

  public static byte[] concat(byte[]... parts) {
    int total = 0;
    for (byte[] p : parts)
      total += p.length;
    byte[] out = new byte[total];
    int off = 0;
    for (byte[] p : parts) {
      System.arraycopy(p, 0, out, off, p.length);
      off += p.length;
    }
    return out;
  }

  public static final class Notification {
    public final String pattern;
    public final String channel;
    public final String message;

    public Notification(String pattern, String channel, String message) {
      this.pattern = pattern;
      this.channel = channel;
      this.message = message;
    }
  }

  /** Routes each received message into a per-channel queue. */
  public static final class CapturingPubSub extends JedisPubSub {

    private final ConcurrentMap<String, BlockingQueue<Notification>> byChannel = new ConcurrentHashMap<>();
    public final CountDownLatch subscribed = new CountDownLatch(1);

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
      subscribed.countDown();
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
      subscribed.countDown();
    }

    @Override
    public void onMessage(String channel, String message) {
      queueFor(channel).add(new Notification(null, channel, message));
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
      queueFor(channel).add(new Notification(pattern, channel, message));
    }

    private BlockingQueue<Notification> queueFor(String channel) {
      return byChannel.computeIfAbsent(channel, k -> new LinkedBlockingQueue<>());
    }

    public Notification expectMessageOn(String channel) throws InterruptedException {
      return expectMessageOn(channel, DEFAULT_AWAIT_MILLIS);
    }

    public Notification expectMessageOn(String channel, long timeoutMillis)
        throws InterruptedException {
      Notification n = queueFor(channel).poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (n == null)
        throw new AssertionError("did not receive notification on channel: " + channel);
      return n;
    }

    public void expectNoMessageOn(String channel, long timeout, TimeUnit unit)
        throws InterruptedException {
      Notification n = queueFor(channel).poll(timeout, unit);
      if (n != null) {
        throw new AssertionError(
            "expected no message on channel '" + channel + "' but received: " + n.message);
      }
    }
  }

  /** Routes each received message into a per-channel queue (binary overload). */
  public static final class CapturingBinaryPubSub extends BinaryJedisPubSub {

    private final ConcurrentMap<ByteBuffer, BlockingQueue<byte[]>> byChannel = new ConcurrentHashMap<>();
    public final CountDownLatch subscribed = new CountDownLatch(1);

    @Override
    public void onSubscribe(byte[] channel, int subscribedChannels) {
      subscribed.countDown();
    }

    @Override
    public void onPSubscribe(byte[] pattern, int subscribedChannels) {
      subscribed.countDown();
    }

    @Override
    public void onMessage(byte[] channel, byte[] message) {
      queueFor(channel).add(message);
    }

    @Override
    public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
      queueFor(channel).add(message);
    }

    private BlockingQueue<byte[]> queueFor(byte[] channel) {
      return byChannel.computeIfAbsent(ByteBuffer.wrap(channel), k -> new LinkedBlockingQueue<>());
    }

    public byte[] expectMessageOn(byte[] channel) throws InterruptedException {
      return expectMessageOn(channel, DEFAULT_AWAIT_MILLIS);
    }

    public byte[] expectMessageOn(byte[] channel, long timeoutMillis) throws InterruptedException {
      byte[] msg = queueFor(channel).poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (msg == null) throw new AssertionError("did not receive notification on expected channel");
      return msg;
    }

    public void expectNoMessageOn(byte[] channel, long timeout, TimeUnit unit)
        throws InterruptedException {
      byte[] msg = queueFor(channel).poll(timeout, unit);
      if (msg != null) {
        throw new AssertionError(
            "expected no message on the given channel but received " + msg.length + " bytes");
      }
    }
  }
}
