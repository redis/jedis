package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.Protocol.Command.CLIENT;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class PublishSubscribeCommandsTest extends JedisCommandsTestBase {

  public PublishSubscribeCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  private void publishOne(final String channel, final String message) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          Jedis j = createJedis();
          j.publish(channel, message);
          j.disconnect();
        } catch (Exception ex) {
        }
      }
    });
    t.start();
  }

  @Test
  public void subscribe() throws InterruptedException {
    jedis.subscribe(new JedisPubSub() {
      public void onMessage(String channel, String message) {
        assertEquals("foo", channel);
        assertEquals("exit", message);
        unsubscribe();
      }

      public void onSubscribe(String channel, int subscribedChannels) {
        assertEquals("foo", channel);
        assertEquals(1, subscribedChannels);

        // now that I'm subscribed... publish
        publishOne("foo", "exit");
      }

      public void onUnsubscribe(String channel, int subscribedChannels) {
        assertEquals("foo", channel);
        assertEquals(0, subscribedChannels);
      }
    }, "foo");
  }

  @Test
  public void pubSubChannels() {
    jedis.subscribe(new JedisPubSub() {
      private int count = 0;

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        count++;
        // All channels are subscribed
        if (count == 3) {
          Jedis otherJedis = createJedis();
          List<String> activeChannels = otherJedis.pubsubChannels();
          // Since we are utilizing sentinel for the tests, there is an additional
          // '__sentinel__:hello' channel that has subscribers and will be returned
          // from PUBSUB CHANNELS.
          assertThat(activeChannels, hasItems("testchan1", "testchan2", "testchan3"));
          unsubscribe();
        }
      }
    }, "testchan1", "testchan2", "testchan3");
  }

  @Test
  public void pubSubChannelsWithPattern() {
    jedis.subscribe(new JedisPubSub() {
      private int count = 0;

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        count++;
        // All channels are subscribed
        if (count == 3) {
          Jedis otherJedis = createJedis();
          List<String> activeChannels = otherJedis.pubsubChannels("test*");
          assertThat(activeChannels, hasItems("testchan1", "testchan2", "testchan3"));
          unsubscribe();
        }
      }
    }, "testchan1", "testchan2", "testchan3");
  }

  @Test
  public void pubSubChannelWithPingPong() throws InterruptedException {
    final CountDownLatch latchUnsubscribed = new CountDownLatch(1);
    final CountDownLatch latchReceivedPong = new CountDownLatch(1);
    jedis.subscribe(new JedisPubSub() {

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        publishOne("testchan1", "hello");
      }

      @Override
      public void onMessage(String channel, String message) {
        this.ping();
      }

      @Override
      public void onPong(String pattern) {
        latchReceivedPong.countDown();
        unsubscribe();
      }

      @Override
      public void onUnsubscribe(String channel, int subscribedChannels) {
        latchUnsubscribed.countDown();
      }
    }, "testchan1");
    assertEquals(0L, latchReceivedPong.getCount());
    assertEquals(0L, latchUnsubscribed.getCount());
  }

  @Test
  public void pubSubChannelWithPingPongWithArgument() throws InterruptedException {
    final CountDownLatch latchUnsubscribed = new CountDownLatch(1);
    final CountDownLatch latchReceivedPong = new CountDownLatch(1);
    final List<String> pongPatterns = new ArrayList<>();
    jedis.subscribe(new JedisPubSub() {

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        publishOne("testchan1", "hello");
      }

      @Override
      public void onMessage(String channel, String message) {
        this.ping("hi!");
      }

      @Override
      public void onPong(String pattern) {
        pongPatterns.add(pattern);
        latchReceivedPong.countDown();
        unsubscribe();
      }

      @Override
      public void onUnsubscribe(String channel, int subscribedChannels) {
        latchUnsubscribed.countDown();
      }
    }, "testchan1");

    assertEquals(0L, latchReceivedPong.getCount());
    assertEquals(0L, latchUnsubscribed.getCount());
    assertEquals(Collections.singletonList("hi!"), pongPatterns);
  }

  @Test
  public void pubSubNumPat() {
    jedis.psubscribe(new JedisPubSub() {
      private int count = 0;

      @Override
      public void onPSubscribe(String pattern, int subscribedChannels) {
        count++;
        if (count == 3) {
          Jedis otherJedis = createJedis();
          Long numPatterns = otherJedis.pubsubNumPat();
          assertEquals(Long.valueOf(2l), numPatterns);
          punsubscribe();
        }
      }

    }, "test*", "test*", "chan*");
  }

  @Test
  public void pubSubNumSub() {
    final Map<String, Long> expectedNumSub = new HashMap<>();
    expectedNumSub.put("testchannel2", 1L);
    expectedNumSub.put("testchannel1", 1L);
    jedis.subscribe(new JedisPubSub() {
      private int count = 0;

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        count++;
        if (count == 2) {
          Jedis otherJedis = createJedis();
          Map<String, Long> numSub = otherJedis.pubsubNumSub("testchannel1", "testchannel2");
          assertEquals(expectedNumSub, numSub);
          unsubscribe();
        }
      }
    }, "testchannel1", "testchannel2");
  }

  @Test
  public void subscribeMany() throws UnknownHostException, IOException, InterruptedException {
    jedis.subscribe(new JedisPubSub() {
      public void onMessage(String channel, String message) {
        unsubscribe(channel);
      }

      public void onSubscribe(String channel, int subscribedChannels) {
        publishOne(channel, "exit");
      }

    }, "foo", "bar");
  }

  @Test
  public void psubscribe() throws UnknownHostException, IOException, InterruptedException {
    jedis.psubscribe(new JedisPubSub() {
      public void onPSubscribe(String pattern, int subscribedChannels) {
        assertEquals("foo.*", pattern);
        assertEquals(1, subscribedChannels);
        publishOne("foo.bar", "exit");

      }

      public void onPUnsubscribe(String pattern, int subscribedChannels) {
        assertEquals("foo.*", pattern);
        assertEquals(0, subscribedChannels);
      }

      public void onPMessage(String pattern, String channel, String message) {
        assertEquals("foo.*", pattern);
        assertEquals("foo.bar", channel);
        assertEquals("exit", message);
        punsubscribe();
      }
    }, "foo.*");
  }

  @Test
  public void psubscribeMany() throws UnknownHostException, IOException, InterruptedException {
    jedis.psubscribe(new JedisPubSub() {
      public void onPSubscribe(String pattern, int subscribedChannels) {
        publishOne(pattern.replace("*", "123"), "exit");
      }

      public void onPMessage(String pattern, String channel, String message) {
        punsubscribe(pattern);
      }
    }, "foo.*", "bar.*");
  }

  @Test
  public void subscribeLazily() throws UnknownHostException, IOException, InterruptedException {
    final JedisPubSub pubsub = new JedisPubSub() {
      public void onMessage(String channel, String message) {
        unsubscribe(channel);
      }

      public void onSubscribe(String channel, int subscribedChannels) {
        publishOne(channel, "exit");
        if (!channel.equals("bar")) {
          this.subscribe("bar");
          this.psubscribe("bar.*");
        }
      }

      public void onPSubscribe(String pattern, int subscribedChannels) {
        publishOne(pattern.replace("*", "123"), "exit");
      }

      public void onPMessage(String pattern, String channel, String message) {
        punsubscribe(pattern);
      }
    };

    jedis.subscribe(pubsub, "foo");
  }

  @Test
  public void binarySubscribe() throws UnknownHostException, IOException, InterruptedException {
    jedis.subscribe(new BinaryJedisPubSub() {
      public void onMessage(byte[] channel, byte[] message) {
        assertArrayEquals(SafeEncoder.encode("foo"), channel);
        assertArrayEquals(SafeEncoder.encode("exit"), message);
        unsubscribe();
      }

      public void onSubscribe(byte[] channel, int subscribedChannels) {
        assertArrayEquals(SafeEncoder.encode("foo"), channel);
        assertEquals(1, subscribedChannels);
        publishOne(SafeEncoder.encode(channel), "exit");
      }

      public void onUnsubscribe(byte[] channel, int subscribedChannels) {
        assertArrayEquals(SafeEncoder.encode("foo"), channel);
        assertEquals(0, subscribedChannels);
      }
    }, SafeEncoder.encode("foo"));
  }

  @Test
  public void binarySubscribeMany() throws UnknownHostException, IOException, InterruptedException {
    jedis.subscribe(new BinaryJedisPubSub() {
      public void onMessage(byte[] channel, byte[] message) {
        unsubscribe(channel);
      }

      public void onSubscribe(byte[] channel, int subscribedChannels) {
        publishOne(SafeEncoder.encode(channel), "exit");
      }
    }, SafeEncoder.encode("foo"), SafeEncoder.encode("bar"));
  }

  @Test
  public void binaryPsubscribe() throws UnknownHostException, IOException, InterruptedException {
    jedis.psubscribe(new BinaryJedisPubSub() {
      public void onPSubscribe(byte[] pattern, int subscribedChannels) {
        assertArrayEquals(SafeEncoder.encode("foo.*"), pattern);
        assertEquals(1, subscribedChannels);
        publishOne(SafeEncoder.encode(pattern).replace("*", "bar"), "exit");
      }

      public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
        assertArrayEquals(SafeEncoder.encode("foo.*"), pattern);
        assertEquals(0, subscribedChannels);
      }

      public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        assertArrayEquals(SafeEncoder.encode("foo.*"), pattern);
        assertArrayEquals(SafeEncoder.encode("foo.bar"), channel);
        assertArrayEquals(SafeEncoder.encode("exit"), message);
        punsubscribe();
      }
    }, SafeEncoder.encode("foo.*"));
  }

  @Test
  public void binaryPsubscribeMany() throws UnknownHostException, IOException, InterruptedException {
    jedis.psubscribe(new BinaryJedisPubSub() {
      public void onPSubscribe(byte[] pattern, int subscribedChannels) {
        publishOne(SafeEncoder.encode(pattern).replace("*", "123"), "exit");
      }

      public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        punsubscribe(pattern);
      }
    }, SafeEncoder.encode("foo.*"), SafeEncoder.encode("bar.*"));
  }

  @Test
  public void binaryPubSubChannelWithPingPong() throws InterruptedException {
    final CountDownLatch latchUnsubscribed = new CountDownLatch(1);
    final CountDownLatch latchReceivedPong = new CountDownLatch(1);

    jedis.subscribe(new BinaryJedisPubSub() {

      @Override
      public void onSubscribe(byte[] channel, int subscribedChannels) {
        publishOne("testchan1", "hello");
      }

      @Override
      public void onMessage(byte[] channel, byte[] message) {
        this.ping();
      }

      @Override
      public void onPong(byte[] pattern) {
        latchReceivedPong.countDown();
        unsubscribe();
      }

      @Override
      public void onUnsubscribe(byte[] channel, int subscribedChannels) {
        latchUnsubscribed.countDown();
      }
    }, SafeEncoder.encode("testchan1"));
    assertEquals(0L, latchReceivedPong.getCount());
    assertEquals(0L, latchUnsubscribed.getCount());
  }

  @Test
  public void binaryPubSubChannelWithPingPongWithArgument() throws InterruptedException {
    final CountDownLatch latchUnsubscribed = new CountDownLatch(1);
    final CountDownLatch latchReceivedPong = new CountDownLatch(1);
    final List<byte[]> pongPatterns = new ArrayList<>();
    final byte[] pingMessage = SafeEncoder.encode("hi!");

    jedis.subscribe(new BinaryJedisPubSub() {

      @Override
      public void onSubscribe(byte[] channel, int subscribedChannels) {
        publishOne("testchan1", "hello");
      }

      @Override
      public void onMessage(byte[] channel, byte[] message) {
        this.ping(pingMessage);
      }

      @Override
      public void onPong(byte[] pattern) {
        pongPatterns.add(pattern);
        latchReceivedPong.countDown();
        unsubscribe();
      }

      @Override
      public void onUnsubscribe(byte[] channel, int subscribedChannels) {
        latchUnsubscribed.countDown();
      }
    }, SafeEncoder.encode("testchan1"));

    assertEquals(0L, latchReceivedPong.getCount());
    assertEquals(0L, latchUnsubscribed.getCount());
    assertArrayEquals(pingMessage, pongPatterns.get(0));
  }

  @Test
  public void binarySubscribeLazily() throws UnknownHostException, IOException,
      InterruptedException {
    final BinaryJedisPubSub pubsub = new BinaryJedisPubSub() {
      public void onMessage(byte[] channel, byte[] message) {
        unsubscribe(channel);
      }

      public void onSubscribe(byte[] channel, int subscribedChannels) {
        publishOne(SafeEncoder.encode(channel), "exit");

        if (!SafeEncoder.encode(channel).equals("bar")) {
          this.subscribe(SafeEncoder.encode("bar"));
          this.psubscribe(SafeEncoder.encode("bar.*"));
        }
      }

      public void onPSubscribe(byte[] pattern, int subscribedChannels) {
        publishOne(SafeEncoder.encode(pattern).replace("*", "123"), "exit");
      }

      public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        punsubscribe(pattern);
      }
    };

    jedis.subscribe(pubsub, SafeEncoder.encode("foo"));
  }

  @Test(expected = JedisException.class)
  public void unsubscribeWhenNotSusbscribed() throws InterruptedException {
    JedisPubSub pubsub = new JedisPubSub() {
    };
    pubsub.unsubscribe();
  }

  @Test(expected = JedisException.class)
  public void handleClientOutputBufferLimitForSubscribeTooSlow() throws InterruptedException {
    final Jedis j = createJedis();
    final AtomicBoolean exit = new AtomicBoolean(false);

    final Thread t = new Thread(new Runnable() {
      public void run() {
        try {

          // we already set jedis1 config to
          // client-output-buffer-limit pubsub 256k 128k 5
          // it means if subscriber delayed to receive over 256k or
          // 128k continuously 5 sec,
          // redis disconnects subscriber

          // we publish over 100M data for making situation for exceed
          // client-output-buffer-limit
          String veryLargeString = makeLargeString(10485760);

          // 10M * 10 = 100M
          for (int i = 0; i < 10 && !exit.get(); i++) {
            j.publish("foo", veryLargeString);
          }

          j.disconnect();
        } catch (Exception ex) {
        }
      }
    });
    t.start();
    try {
      jedis.subscribe(new JedisPubSub() {
        public void onMessage(String channel, String message) {
          try {
            // wait 0.5 secs to slow down subscribe and
            // client-output-buffer exceed
            Thread.sleep(100);
          } catch (Exception e) {
            try {
              t.join();
            } catch (InterruptedException e1) {
            }

            fail(e.getMessage());
          }
        }
      }, "foo");
    } finally {
      // exit the publisher thread. if exception is thrown, thread might
      // still keep publishing things.
      exit.set(true);
      if (t.isAlive()) {
        t.join();
      }
    }
  }

  private String makeLargeString(int size) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < size; i++)
      sb.append((char) ('a' + i % 26));

    return sb.toString();
  }

  @Test(timeout = 5000)
  public void subscribeCacheInvalidateChannel() {
    org.junit.Assume.assumeThat(protocol, Matchers.not(RedisProtocol.RESP3));

    final String cacheInvalidate = "__redis__:invalidate";
    final AtomicBoolean onMessage = new AtomicBoolean(false);
    final JedisPubSub pubsub = new JedisPubSub() {
      @Override public void onMessage(String channel, String message) {
        onMessage.set(true);
        assertEquals(cacheInvalidate, channel);
        if (message != null) {
          assertEquals("foo", message);
          consumeJedis(j -> j.flushAll());
        } else {
          unsubscribe(channel);
        }
      }

      @Override public void onSubscribe(String channel, int subscribedChannels) {
        assertEquals(cacheInvalidate, channel);
        consumeJedis(j -> j.set("foo", "bar"));
      }
    };

    try (Jedis subscriber = createJedis()) {
      long clientId = subscriber.clientId();
      subscriber.sendCommand(CLIENT, "TRACKING", "ON", "REDIRECT", Long.toString(clientId), "BCAST");
      subscriber.subscribe(pubsub, cacheInvalidate);
      assertTrue("Subscriber didn't get any message.", onMessage.get());
    }
  }

  @Test(timeout = 5000)
  public void subscribeCacheInvalidateChannelBinary() {
    org.junit.Assume.assumeThat(protocol, Matchers.not(RedisProtocol.RESP3));

    final byte[] cacheInvalidate = "__redis__:invalidate".getBytes();
    final AtomicBoolean onMessage = new AtomicBoolean(false);
    final BinaryJedisPubSub pubsub = new BinaryJedisPubSub() {
      @Override public void onMessage(byte[] channel, byte[] message) {
        onMessage.set(true);
        assertArrayEquals(cacheInvalidate, channel);
        if (message != null) {
          assertArrayEquals("foo".getBytes(), message);
          consumeJedis(j -> j.flushAll());
        } else {
          unsubscribe(channel);
        }
      }

      @Override public void onSubscribe(byte[] channel, int subscribedChannels) {
        assertArrayEquals(cacheInvalidate, channel);
        consumeJedis(j -> j.set("foo".getBytes(), "bar".getBytes()));
      }
    };

    try (Jedis subscriber = createJedis()) {
      long clientId = subscriber.clientId();
      subscriber.sendCommand(CLIENT, "TRACKING", "ON", "REDIRECT", Long.toString(clientId), "BCAST");
      subscriber.subscribe(pubsub, cacheInvalidate);
      assertTrue("Subscriber didn't get any message.", onMessage.get());
    }
  }

  private void consumeJedis(Consumer<Jedis> consumer) {
    Thread t = new Thread(() -> consumer.accept(jedis));
    t.start();
  }
}
