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
      Arrays.asList(HostAndPorts.getRedisServers().get(2), HostAndPorts.getRedisServers().get(3));
  protected static final Set<String> nodesPorts = nodes.stream()
      .map(HostAndPort::getPort).map(String::valueOf).collect(Collectors.toSet());
  protected static final Set<String> dockerNodeIps = nodesPorts.stream()
          .map(port -> String.format("172.21.0.%d", 10 + Integer.parseInt(port) - 6379)).collect(Collectors.toSet());

  protected static final List<HostAndPort> sentinels2 = 
      Arrays.asList(HostAndPorts.getSentinelServers().get(1), HostAndPorts.getSentinelServers().get(3));

  @Test
  public void myIdAndSentinels() {
    Map<String, String> idToDockerIp = new HashMap<>();
    sentinels2.forEach((hap) -> {
      try (Jedis sentinel = new Jedis(hap)) {
        String id = sentinel.sentinelMyId();
        assertThat(id, Matchers.not(Matchers.emptyOrNullString()));
        idToDockerIp.put(id, String.format("172.21.0.%d", 31 + hap.getPort() - 26379));
      }
    });
    assertEquals(2, idToDockerIp.size());

    try (Jedis sentinel = new Jedis(sentinels2.stream().findAny().get())) {
      List<Map<String, String>> detailsList = sentinel.sentinelSentinels(MASTER_NAME);
      assertThat(detailsList, Matchers.not(Matchers.empty()));
      detailsList.forEach((details) -> {
        assertEquals("26379", details.get("port"));
        assertEquals(idToDockerIp.get(details.get("runid")), details.get("ip"));
      });
    }
  }

  @Test
  public void masterAndMasters() {
    String runId, port, ip;
    try (Jedis sentinel = new Jedis(sentinels2.get(0))) {
      Map<String, String> details = sentinel.sentinelMaster(MASTER_NAME);
      assertEquals(MASTER_NAME, details.get("name"));
      runId = details.get("runid");
      port = details.get("port");
      ip = details.get("ip");
      assertThat(ip, Matchers.in(dockerNodeIps));
      assertEquals("6379", port);
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
          -> assertThat(details.get("ip"), Matchers.in(dockerNodeIps)));
      detailsList.forEach((details)
              -> assertThat(details.get("port"), Matchers.equalTo("6379")));
    }
  }
}
