package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import redis.clients.jedis.JedisPubSub;

public class ClusterValuesCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void testHincrByFloat() {
    Double value = jedisCluster.hincrByFloat("foo", "bar", 1.5d);
    assertEquals((Double) 1.5d, value);
    value = jedisCluster.hincrByFloat("foo", "bar", -1.5d);
    assertEquals((Double) 0d, value);
    value = jedisCluster.hincrByFloat("foo", "bar", -10.7d);
    assertEquals(Double.valueOf(-10.7d), value);
  }

  private void publishOne(final String channel, final String message) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          jedisCluster.publish(channel, message);
        } catch (Exception ex) {
        }
      }
    });
    t.start();
  }

  @Test
  public void subscribe() throws InterruptedException {
    jedisCluster.subscribe(new JedisPubSub() {
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
}
