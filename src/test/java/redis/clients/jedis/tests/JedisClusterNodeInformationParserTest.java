package redis.clients.jedis.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;

public class JedisClusterNodeInformationParserTest extends Assert {
  private ClusterNodeInformationParser parser;

  @Before
  public void setUp() {
    parser = new ClusterNodeInformationParser();
  }

  @Test
  public void testParseNodeMyself() {
    String nodeInfo = "9b0d2ab38ee31482c95fdb2c7847a0d40e88d518 :7379 myself,master - 0 0 1 connected 0-5460";
    HostAndPort current = new HostAndPort("localhost", 7379);
    ClusterNodeInformation clusterNodeInfo = parser.parse(nodeInfo, current);
    assertEquals(clusterNodeInfo.getNode(), current);
  }

  @Test
  public void testParseNormalState() {
    String nodeInfo = "5f4a2236d00008fba7ac0dd24b95762b446767bd 192.168.0.3:7380 master - 0 1400598804016 2 connected 5461-10922";
    HostAndPort current = new HostAndPort("localhost", 7379);
    ClusterNodeInformation clusterNodeInfo = parser.parse(nodeInfo, current);
    assertNotEquals(clusterNodeInfo.getNode(), current);
    assertEquals(clusterNodeInfo.getNode(), new HostAndPort("192.168.0.3", 7380));

    for (int slot = 5461; slot <= 10922; slot++) {
      assertTrue(clusterNodeInfo.getAvailableSlots().contains(slot));
    }

    assertTrue(clusterNodeInfo.getSlotsBeingImported().isEmpty());
    assertTrue(clusterNodeInfo.getSlotsBeingMigrated().isEmpty());
  }

  @Test
  public void testParseSlotBeingMigrated() {
    String nodeInfo = "5f4a2236d00008fba7ac0dd24b95762b446767bd :7379 myself,master - 0 0 1 connected 0-5459 [5460->-5f4a2236d00008fba7ac0dd24b95762b446767bd] [5461-<-5f4a2236d00008fba7ac0dd24b95762b446767bd]";
    HostAndPort current = new HostAndPort("localhost", 7379);
    ClusterNodeInformation clusterNodeInfo = parser.parse(nodeInfo, current);
    assertEquals(clusterNodeInfo.getNode(), current);

    for (int slot = 0; slot <= 5459; slot++) {
      assertTrue(clusterNodeInfo.getAvailableSlots().contains(slot));
    }

    assertEquals(1, clusterNodeInfo.getSlotsBeingMigrated().size());
    assertTrue(clusterNodeInfo.getSlotsBeingMigrated().contains(5460));
    assertEquals(1, clusterNodeInfo.getSlotsBeingImported().size());
    assertTrue(clusterNodeInfo.getSlotsBeingImported().contains(5461));
  }

}
