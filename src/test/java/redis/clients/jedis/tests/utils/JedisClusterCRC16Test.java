package redis.clients.jedis.tests.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;

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
}
