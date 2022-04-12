package redis.clients.jedis.util;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.FailoverAbortedException;
import redis.clients.jedis.exceptions.JedisDataException;

public class JedisSentinelTestUtil {

  public static HostAndPort waitForNewPromotedMaster(final String masterName,
      final Jedis sentinelJedis, final Jedis commandJedis) throws InterruptedException {

    final AtomicReference<String> newmaster = new AtomicReference<String>("");

    sentinelJedis.psubscribe(new JedisPubSub() {

      @Override
      public void onPMessage(String pattern, String channel, String message) {
        if (channel.equals("+switch-master")) {
          newmaster.set(message);
          punsubscribe();
        } else if (channel.startsWith("-failover-abort")) {
          punsubscribe();
          throw new FailoverAbortedException("Unfortunately sentinel cannot failover..."
              + " reason(channel) : " + channel + " / message : " + message);
        }
      }

      @Override
      public void onPSubscribe(String pattern, int subscribedChannels) {
        try {
          commandJedis.sentinelFailover(masterName);
        } catch (JedisDataException je) {
          Assert.assertEquals("INPROG Failover already in progress", je.getMessage());
        }
      }
    }, "*");

    String[] chunks = newmaster.get().split(" ");
    HostAndPort newMaster = new HostAndPort(chunks[3], Integer.parseInt(chunks[4]));

    return newMaster;
  }

}
