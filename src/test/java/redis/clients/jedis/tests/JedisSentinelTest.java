package redis.clients.jedis.tests;

import static junit.framework.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import redis.clients.jedis.Jedis;

public class JedisSentinelTest {

  private static final String MASTER_NAME = "mymaster";

  /**
   * Based on redis/master/slave/sentinel configs from
   * https://github.com/noise/redis-sentinel-tests.
   */
  @Test
  public void sentinel() {
    Jedis j = new Jedis("localhost", 26379);
    List<Map<String, String>> masters = j.sentinelMasters();
    final String masterName = masters.get(0).get("name");

    assertEquals(MASTER_NAME, masterName);

    List<String> masterHostAndPort = j.sentinelGetMasterAddrByName(masterName);
    assertEquals("127.0.0.1", masterHostAndPort.get(0));
    assertEquals("6379", masterHostAndPort.get(1));

    List<Map<String, String>> slaves = j.sentinelSlaves(masterName);
    assertEquals("6379", slaves.get(0).get("master-port"));

    List<? extends Object> isMasterDownByAddr = j.sentinelIsMasterDownByAddr("127.0.0.1", 6379);
    assertEquals(Long.valueOf(0), (Long) isMasterDownByAddr.get(0));
    assertFalse("?".equals(isMasterDownByAddr.get(1)));

    isMasterDownByAddr = j.sentinelIsMasterDownByAddr("127.0.0.1", 1);
    assertEquals(Long.valueOf(0), (Long) isMasterDownByAddr.get(0));
    assertTrue("?".equals(isMasterDownByAddr.get(1)));

    // DO NOT RE-RUN TEST TOO FAST, RESET TAKES SOME TIME TO... RESET
    assertEquals(Long.valueOf(1), j.sentinelReset(masterName));
    assertEquals(Long.valueOf(0), j.sentinelReset("woof" + masterName));
  }
}
