package redis.clients.jedis.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;

import java.util.ArrayList;
import java.util.List;

public class JedisClusterNodeInformationParserTest extends Assert {
  private ClusterNodeInformationParser parser;

  @Before
  public void setUp() {
    parser = new ClusterNodeInformationParser();
  }

  @Test
  public void testParseNormalState() {
    List<Object> nodeInfo = new ArrayList<Object>();
    List<Object> nodeData = new ArrayList<Object>();
    nodeData.add("localhost".getBytes());
    nodeData.add(new Long(7380));

    nodeInfo.add(new Long(5461));
    nodeInfo.add(new Long(10922));
    nodeInfo.add(nodeData);

    ClusterNodeInformation clusterNodeInfo = parser.parse(nodeInfo);
    assertEquals(clusterNodeInfo.getNode(), new HostAndPort("localhost", 7380));

    for (int slot = 5461; slot <= 10922; slot++) {
      assertTrue(clusterNodeInfo.getAvailableSlots().contains(slot));
    }

  }

}
