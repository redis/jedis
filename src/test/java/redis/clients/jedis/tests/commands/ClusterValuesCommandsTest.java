package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
