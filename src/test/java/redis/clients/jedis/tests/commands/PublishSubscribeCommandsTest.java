package redis.clients.jedis.tests.commands;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class PublishSubscribeCommandsTest extends JedisCommandTestBase {
    @Test
    public void subscribe() throws UnknownHostException, IOException,
	    InterruptedException {
	new Thread(new Runnable() {
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
	}).start();
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
    }

    @Test
    public void subscribeMany() throws UnknownHostException, IOException,
	    InterruptedException {
	new Thread(new Runnable() {
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
	}).start();
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
    }

    @Test
    public void psubscribe() throws UnknownHostException, IOException,
	    InterruptedException {
	new Thread(new Runnable() {
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
	}).start();
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
    }

    @Test
    public void psubscribeMany() throws UnknownHostException, IOException,
	    InterruptedException {
	new Thread(new Runnable() {
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
	}).start();
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
    }
}