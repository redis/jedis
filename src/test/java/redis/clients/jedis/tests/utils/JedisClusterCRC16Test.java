package redis.clients.jedis.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

public class JedisClusterCRC16Test {

  @Test
  public void testGetCRC16() throws Exception {
    Map<String, Integer> solutions = prepareSolutionSet();

    for (Entry<String, Integer> entry : solutions.entrySet()) {
      // string version
      assertEquals(entry.getValue().intValue(), JedisClusterCRC16.getCRC16(entry.getKey()));

      // byte array version
      assertEquals(entry.getValue().intValue(),
        JedisClusterCRC16.getCRC16(SafeEncoder.encode(entry.getKey())));
    }
  }

  @Test
  public void testGetSlot() {
    assertEquals(7186, JedisClusterCRC16.getSlot("51"));
  }

  private Map<String, Integer> prepareSolutionSet() {
    Map<String, Integer> solutionMap = new HashMap<String, Integer>();
    solutionMap.put("", 0x0);
    solutionMap.put("123456789", 0x31C3);
    solutionMap.put("sfger132515", 0xA45C);
    solutionMap.put("hae9Napahngaikeethievubaibogiech", 0x58CE);
    solutionMap.put("AAAAAAAAAAAAAAAAAAAAAA", 0x92cd);
    solutionMap.put("Hello, World!", 0x4FD6);
    return solutionMap;
  }

  @Test
  public void testRedisHashtagGetSlot() {
    assertEquals(JedisClusterCRC16.getSlot("{bar"), JedisClusterCRC16.getSlot("foo{{bar}}zap"));
    assertEquals(JedisClusterCRC16.getSlot("{user1000}.following"),
      JedisClusterCRC16.getSlot("{user1000}.followers"));
    assertNotEquals(JedisClusterCRC16.getSlot("foo{}{bar}"), JedisClusterCRC16.getSlot("bar"));
    assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}"), JedisClusterCRC16.getSlot("bar"));
  }

  @Test
  public void testBinaryHashtagGetSlot() {
    assertEquals(JedisClusterCRC16.getSlot("{bar".getBytes()), JedisClusterCRC16.getSlot("{bar".getBytes()));
    assertEquals(JedisClusterCRC16.getSlot("{user1000}.following".getBytes()),
      JedisClusterCRC16.getSlot("{user1000}.followers".getBytes()));
    assertNotEquals(JedisClusterCRC16.getSlot("foo{}{bar}".getBytes()),
      JedisClusterCRC16.getSlot("bar".getBytes()));
    assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}".getBytes()),
      JedisClusterCRC16.getSlot("bar".getBytes()));
  }

}
