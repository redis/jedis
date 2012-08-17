package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;

public class PublishSubscribeCommandsTest extends JedisCommandTestBase {
    @Test
    public void subscribe() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish("foo", "exit");
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.subscribe(new JedisPubSub() {
            public void onMessage(String channel, String message) {
                assertEquals("foo", channel);
                assertEquals("exit", message);
                unsubscribe();
            }

            public void onSubscribe(String channel, int subscribedChannels) {
                assertEquals("foo", channel);
                assertEquals(1, subscribedChannels);
            }

            public void onUnsubscribe(String channel, int subscribedChannels) {
                assertEquals("foo", channel);
                assertEquals(0, subscribedChannels);
            }

            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            public void onPMessage(String pattern, String channel,
                    String message) {
            }
        }, "foo");
        t.join();
    }

    @Test
    public void subscribeMany() throws UnknownHostException, IOException,
            InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish("foo", "exit");
                    Thread.sleep(1000);
                    j.publish("bar", "exit");
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.subscribe(new JedisPubSub() {
            public void onMessage(String channel, String message) {
                unsubscribe(channel);
            }

            public void onSubscribe(String channel, int subscribedChannels) {
            }

            public void onUnsubscribe(String channel, int subscribedChannels) {
            }

            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            public void onPMessage(String pattern, String channel,
                    String message) {
            }
        }, "foo", "bar");
        t.join();
    }

    @Test
    public void psubscribe() throws UnknownHostException, IOException,
            InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish("foo.bar", "exit");
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.psubscribe(new JedisPubSub() {
            public void onMessage(String channel, String message) {
            }

            public void onSubscribe(String channel, int subscribedChannels) {
            }

            public void onUnsubscribe(String channel, int subscribedChannels) {
            }

            public void onPSubscribe(String pattern, int subscribedChannels) {
                assertEquals("foo.*", pattern);
                assertEquals(1, subscribedChannels);
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels) {
                assertEquals("foo.*", pattern);
                assertEquals(0, subscribedChannels);
            }

            public void onPMessage(String pattern, String channel,
                    String message) {
                assertEquals("foo.*", pattern);
                assertEquals("foo.bar", channel);
                assertEquals("exit", message);
                punsubscribe();
            }
        }, "foo.*");
        t.join();
    }

    @Test
    public void psubscribeMany() throws UnknownHostException, IOException,
            InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish("foo.123", "exit");
                    Thread.sleep(1000);
                    j.publish("bar.123", "exit");
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.psubscribe(new JedisPubSub() {
            public void onMessage(String channel, String message) {
            }

            public void onSubscribe(String channel, int subscribedChannels) {
            }

            public void onUnsubscribe(String channel, int subscribedChannels) {
            }

            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            public void onPMessage(String pattern, String channel,
                    String message) {
                punsubscribe(pattern);
            }
        }, "foo.*", "bar.*");
        t.join();
    }

    @Test
    public void subscribeLazily() throws UnknownHostException, IOException,
            InterruptedException {
        final JedisPubSub pubsub = new JedisPubSub() {
            public void onMessage(String channel, String message) {
                unsubscribe(channel);
            }

            public void onSubscribe(String channel, int subscribedChannels) {
            }

            public void onUnsubscribe(String channel, int subscribedChannels) {
            }

            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            public void onPMessage(String pattern, String channel,
                    String message) {
                punsubscribe(pattern);
            }
        };

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    pubsub.subscribe("bar");
                    pubsub.psubscribe("bar.*");
                    j.publish("foo", "exit");
                    j.publish("bar", "exit");
                    j.publish("bar.123", "exit");
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.subscribe(pubsub, "foo");
        t.join();
    }

    @Test
    public void binarySubscribe() throws UnknownHostException, IOException,
            InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish(SafeEncoder.encode("foo"), SafeEncoder
                            .encode("exit"));
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.subscribe(new BinaryJedisPubSub() {
            public void onMessage(byte[] channel, byte[] message) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo"), channel));
                assertTrue(Arrays.equals(SafeEncoder.encode("exit"), message));
                unsubscribe();
            }

            public void onSubscribe(byte[] channel, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo"), channel));
                assertEquals(1, subscribedChannels);
            }

            public void onUnsubscribe(byte[] channel, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo"), channel));
                assertEquals(0, subscribedChannels);
            }

            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPMessage(byte[] pattern, byte[] channel,
                    byte[] message) {
            }
        }, SafeEncoder.encode("foo"));
        t.join();
    }

    @Test
    public void binarySubscribeMany() throws UnknownHostException, IOException,
            InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish(SafeEncoder.encode("foo"), SafeEncoder
                            .encode("exit"));
                    Thread.sleep(1000);
                    j.publish(SafeEncoder.encode("bar"), SafeEncoder
                            .encode("exit"));
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.subscribe(new BinaryJedisPubSub() {
            public void onMessage(byte[] channel, byte[] message) {
                unsubscribe(channel);
            }

            public void onSubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPMessage(byte[] pattern, byte[] channel,
                    byte[] message) {
            }
        }, SafeEncoder.encode("foo"), SafeEncoder.encode("bar"));
        t.join();
    }

    @Test
    public void binaryPsubscribe() throws UnknownHostException, IOException,
            InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish(SafeEncoder.encode("foo.bar"), SafeEncoder
                            .encode("exit"));
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.psubscribe(new BinaryJedisPubSub() {
            public void onMessage(byte[] channel, byte[] message) {
            }

            public void onSubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.*"), pattern));
                assertEquals(1, subscribedChannels);
            }

            public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.*"), pattern));
                assertEquals(0, subscribedChannels);
            }

            public void onPMessage(byte[] pattern, byte[] channel,
                    byte[] message) {
                assertTrue(Arrays.equals(SafeEncoder.encode("foo.*"), pattern));
                assertTrue(Arrays
                        .equals(SafeEncoder.encode("foo.bar"), channel));
                assertTrue(Arrays.equals(SafeEncoder.encode("exit"), message));
                punsubscribe();
            }
        }, SafeEncoder.encode("foo.*"));
        t.join();
    }

    @Test
    public void binaryPsubscribeMany() throws UnknownHostException,
            IOException, InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    j.publish(SafeEncoder.encode("foo.123"), SafeEncoder
                            .encode("exit"));
                    Thread.sleep(1000);
                    j.publish(SafeEncoder.encode("bar.123"), SafeEncoder
                            .encode("exit"));
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.psubscribe(new BinaryJedisPubSub() {
            public void onMessage(byte[] channel, byte[] message) {
            }

            public void onSubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPMessage(byte[] pattern, byte[] channel,
                    byte[] message) {
                punsubscribe(pattern);
            }
        }, SafeEncoder.encode("foo.*"), SafeEncoder.encode("bar.*"));
        t.join();
    }

    @Test
    public void binarySubscribeLazily() throws UnknownHostException,
            IOException, InterruptedException {
        final BinaryJedisPubSub pubsub = new BinaryJedisPubSub() {
            public void onMessage(byte[] channel, byte[] message) {
                unsubscribe(channel);
            }

            public void onSubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            }

            public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            }

            public void onPMessage(byte[] pattern, byte[] channel,
                    byte[] message) {
                punsubscribe(pattern);
            }
        };

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Jedis j = createJedis();
                    Thread.sleep(1000);
                    pubsub.subscribe(SafeEncoder.encode("bar"));
                    pubsub.psubscribe(SafeEncoder.encode("bar.*"));
                    j.publish(SafeEncoder.encode("foo"), SafeEncoder
                            .encode("exit"));
                    j.publish(SafeEncoder.encode("bar"), SafeEncoder
                            .encode("exit"));
                    j.publish(SafeEncoder.encode("bar.123"), SafeEncoder
                            .encode("exit"));
                    j.disconnect();
                } catch (Exception ex) {
                    fail(ex.getMessage());
                }
            }
        });
        t.start();
        jedis.subscribe(pubsub, SafeEncoder.encode("foo"));
        t.join();
    }

    @Test @Ignore
    public void subscribeWithoutConnecting() {
        try {
            Jedis jedis = new Jedis(hnp.host, hnp.port);
            jedis.subscribe(new JedisPubSub() {
                public void onMessage(String channel, String message) {
                }

                public void onPMessage(String pattern, String channel,
                        String message) {
                }

                public void onSubscribe(String channel, int subscribedChannels) {
                }

                public void onUnsubscribe(String channel, int subscribedChannels) {
                }

                public void onPUnsubscribe(String pattern,
                        int subscribedChannels) {
                }

                public void onPSubscribe(String pattern, int subscribedChannels) {
                }
            }, "foo");
        } catch (NullPointerException ex) {
            fail();
        } catch (JedisDataException ex) {
            // this is OK because we are not sending AUTH command
        }
    }

    @Test(expected = JedisConnectionException.class)
    public void unsubscribeWhenNotSusbscribed() throws InterruptedException {
        JedisPubSub pubsub = new JedisPubSub() {
            public void onMessage(String channel, String message) {
            }

            public void onPMessage(String pattern, String channel,
                    String message) {
            }

            public void onSubscribe(String channel, int subscribedChannels) {
            }

            public void onUnsubscribe(String channel, int subscribedChannels) {
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            public void onPSubscribe(String pattern, int subscribedChannels) {
            }
        };
        pubsub.unsubscribe();
    }
}
