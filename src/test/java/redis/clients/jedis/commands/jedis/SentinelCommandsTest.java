package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.HostAndPorts;

public class SentinelCommandsTest {

  protected static final String MASTER_NAME = "mymaster";

  protected static final List<HostAndPort> nodes =
      Arrays.asList(
          HostAndPorts.getRedisEndpoint("standalone2-primary").getHostAndPort(),
          HostAndPorts.getRedisEndpoint("standalone3-replica-of-standalone2").getHostAndPort());
  protected static final Set<String> nodesPorts = nodes.stream()
      .map(HostAndPort::getPort).map(String::valueOf).collect(Collectors.toSet());

  protected static final List<HostAndPort> sentinels2 = 
      Arrays.asList(HostAndPorts.getSentinelServers().get(1), HostAndPorts.getSentinelServers().get(3));

  @Test
  public void myIdAndSentinels() {
    Map<String, Integer> idToPort = new HashMap<>();
    sentinels2.forEach((hap) -> {
      try (Jedis sentinel = new Jedis(hap)) {
        String id = sentinel.sentinelMyId();
        assertThat(id, Matchers.not(Matchers.emptyOrNullString()));
        idToPort.put(id, hap.getPort());
      }
    });
    assertEquals(2, idToPort.size());

    try (Jedis sentinel = new Jedis(sentinels2.stream().findAny().get())) {
      List<Map<String, String>> detailsList = sentinel.sentinelSentinels(MASTER_NAME);
      assertThat(detailsList, Matchers.not(Matchers.empty()));
      detailsList.forEach((details)
          -> assertEquals(idToPort.get(details.get("runid")),
              Integer.valueOf(details.get("port"))));
    }
  }

  @Test
  public void masterAndMasters() {
    String runId, port;
    try (Jedis sentinel = new Jedis(sentinels2.get(0))) {
      Map<String, String> details = sentinel.sentinelMaster(MASTER_NAME);
      assertEquals(MASTER_NAME, details.get("name"));
      runId = details.get("runid");
      port = details.get("port");
      assertThat(port, Matchers.in(nodesPorts));
    }

    try (Jedis sentinel2 = new Jedis(sentinels2.get(1))) {
      Map<String, String> details = sentinel2.sentinelMasters().get(0);
      assertEquals(MASTER_NAME, details.get("name"));
      assertEquals(runId, details.get("runid"));
      assertEquals(port, details.get("port"));
    }
  }

  @Test
  public void replicas() {
    try (Jedis sentinel = new Jedis(sentinels2.stream().findAny().get())) {
      List<Map<String, String>> detailsList = sentinel.sentinelReplicas(MASTER_NAME);
      assertThat(detailsList, Matchers.not(Matchers.empty()));
      detailsList.forEach((details)
          -> assertThat(details.get("port"), Matchers.in(nodesPorts)));
    }
  }
}
