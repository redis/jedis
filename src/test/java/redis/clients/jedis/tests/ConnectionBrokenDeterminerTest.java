package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionBrokenDeterminerTest extends JedisCommandTestBase {

  @Test
  public void testCustomExceptionHandlerShouldMarkConnectionAsBroken() {
    JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), jedis.getClient().getHost(),
        jedis.getClient().getPort(), 2000, "foobared");

    ConnectionBrokenDeterminer determiner = new ConnectionBrokenDeterminer();
    determiner.addPattern(new ConnectionBrokenPattern() {
      @Override
      public boolean determine(RuntimeException throwable) {
        // It covers "Wrong number of args calling Redis command From Lua script"
        if (throwable instanceof JedisDataException) {
          JedisDataException e = (JedisDataException) throwable;
          if (e.getMessage().contains("Wrong number of args")) {
            return true;
          }
        }
        return false;
      }
    });

    jedisPool.setConnectionBrokenDeterminer(determiner);
    Jedis jedis2 = null;

    try {
      jedis2 = jedisPool.getResource();

      jedis2.eval("return redis.pcall('hset', 'a', 'b')");
      fail("Should raise JedisConnectionException");
    } catch (JedisConnectionException e) {
      assertNotNull(jedis2);
      assertTrue(jedis2.getClient().isBroken());
    } catch (Throwable e) {
      fail("Should handle certain exception to JedisConnectionException");
    } finally {
      jedis2.close();
      jedisPool.close();
    }
  }
}
