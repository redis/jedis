package redis.clients.jedis.scenario;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

@Tags({ @Tag("scenario") })
public class ClusterTopologyRefreshIT {

  private static final Logger log = LoggerFactory.getLogger(ClusterTopologyRefreshIT.class);

  private static EndpointConfig endpoint;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @BeforeAll
  public static void beforeClass() {
    try {
      ClusterTopologyRefreshIT.endpoint = HostAndPorts
          .getRedisEndpoint("re-single-shard-oss-cluster");
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      assumeTrue(false);
    }
  }

  @Test
  public void testWithPool() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(endpoint.getHostAndPort());

    JedisClientConfig config = endpoint.getClientConfigBuilder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    try (RedisClusterClient client = RedisClusterClient.builder().nodes(jedisClusterNode)
        .clientConfig(config).maxAttempts(RecommendedSettings.MAX_RETRIES)
        .maxTotalRetriesDuration(RecommendedSettings.MAX_TOTAL_RETRIES_DURATION).build()) {
      Set<String> initialNodes = client.getClusterNodes().keySet();
      // assertEquals(1, initialNodes.size(),
      // "Was this BDB used to run this test before?");

      AtomicLong commandsExecuted = new AtomicLong();

      // Start thread that imitates an application that uses the client
      FakeApp fakeApp = new FakeApp(client, (UnifiedJedis c) -> {
        long i = commandsExecuted.getAndIncrement();
        client.set(String.valueOf(i), String.valueOf(i));
        return true;
      });

      Thread t = new Thread(fakeApp);
      t.start();

      HashMap<String, Object> params = new HashMap<>();
      params.put("bdb_id", endpoint.getBdbId());
      params.put("actions", "[\"reshard\",\"failover\"]");

      FaultInjectionClient.TriggerActionResponse actionResponse = null;

      try {
        log.info("Triggering Resharding and Failover");
        actionResponse = faultClient.triggerAction("sequence_of_actions", params);
      } catch (IOException e) {
        fail("Fault Injection Server error:" + e.getMessage());
      }

      log.info("Action id: {}", actionResponse.getActionId());
      fakeApp.setAction(actionResponse);

      try {
        t.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      assertTrue(fakeApp.capturedExceptions().isEmpty());

      log.info("Commands executed: {}", commandsExecuted.get());
      for (long i = 0; i < commandsExecuted.get(); i++) {
        assertTrue(client.exists(String.valueOf(i)));
      }

      Set<String> afterReshardNodes = client.getClusterNodes().keySet();
      assertThat("After set should have more nodes than initial set", afterReshardNodes.size(),
        greaterThan(initialNodes.size()));

      boolean hasNewNode = afterReshardNodes.stream().anyMatch(n -> !initialNodes.contains(n));
      assertThat("After set should have a node not in initial set", hasNewNode, is(true));
    }
  }

}
