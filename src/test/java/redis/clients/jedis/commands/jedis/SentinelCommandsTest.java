package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.HostAndPorts;

public class SentinelCommandsTest {

  protected static HostAndPort master2 = HostAndPorts.getRedisServers().get(2);
  protected static HostAndPort replica2 = HostAndPorts.getRedisServers().get(3);

  protected static HostAndPort sentinel2_1 = HostAndPorts.getSentinelServers().get(1);
  protected static HostAndPort sentinel2_2 = HostAndPorts.getSentinelServers().get(3);

  @Test
  public void myIdSentinels() {
    String id1;
    try (Jedis sentinel = new Jedis(sentinel2_1)) {
      id1 = sentinel.sentinelMyId();
      assertTrue(id1.matches("[0-9a-f]+"));
    }

    try (Jedis sentinel2 = new Jedis(sentinel2_2)) {
      Map<String, String> details1 = sentinel2.sentinelSentinels("mymaster").get(0);
      assertEquals(id1, details1.get("runid"));
    }
  }

  @Test
  public void masterMasters() {
    String runId;
    try (Jedis sentinel = new Jedis(sentinel2_1)) {
      Map<String, String> details = sentinel.sentinelMaster("mymaster");
      assertEquals("mymaster", details.get("name"));
      runId = details.get("runid");
    }

    try (Jedis sentinel2 = new Jedis(sentinel2_2)) {
      Map<String, String> details = sentinel2.sentinelMasters().get(0);
      assertEquals("mymaster", details.get("name"));
      assertEquals(runId, details.get("runid"));
    }
  }

  @Test
  public void replicas() {
    try (Jedis sentinel = new Jedis(sentinel2_1)) {
      List<Map<String, String>> replicas = sentinel.sentinelReplicas("mymaster");
      for(Map<String, String> replica:replicas){
        assertEquals("slave",replica.get("role-reported"));
      }
    }
  }
}
