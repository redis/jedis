package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Endpoints;

@Tag("integration")
@ResourceLocks({
    @ResourceLock(value = Endpoints.STANDALONE2_PRIMARY),
    @ResourceLock(value = Endpoints.STANDALONE3_REPLICA_OF_STANDALONE2),
    @ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_1),
    @ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_3)
})
public class SentinelCommandsTest {

  protected static final String MASTER_NAME = "mymaster";

  protected static List<HostAndPort> nodes;
  protected static Set<String> nodesPorts;

  protected static List<HostAndPort> sentinels2;

  @BeforeAll
  public static void prepareEndpoints() {
    nodes = Arrays.asList(
        Endpoints.getRedisEndpoint(Endpoints.STANDALONE2_PRIMARY).getHostAndPort(),
        Endpoints.getRedisEndpoint(Endpoints.STANDALONE3_REPLICA_OF_STANDALONE2).getHostAndPort());
    nodesPorts = nodes.stream()
        .map(HostAndPort::getPort).map(String::valueOf).collect(Collectors.toSet());
    sentinels2 = Arrays.asList(
        Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_1).getHostAndPort(),
        Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_3).getHostAndPort());
  }

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
