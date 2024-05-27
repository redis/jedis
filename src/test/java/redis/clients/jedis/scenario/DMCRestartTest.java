package redis.clients.jedis.scenario;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.misc.AutomaticFailoverTest;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

import static org.junit.Assert.*;

public class DMCRestartTest {

  private static final Logger log = LoggerFactory.getLogger(AutomaticFailoverTest.class);

  private final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("re-standalone");

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @Test
  public void testWithPool() {

    // Validate recommended configuration
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(8);
    poolConfig.setMaxIdle(8);
    poolConfig.setMinIdle(0);
    poolConfig.setBlockWhenExhausted(true);
    poolConfig.setMaxWait(Duration.ofSeconds(1));
    poolConfig.setTestWhileIdle(true);
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));

    // Retry settings
    int maxAttempts = 3;
    Duration maxTotalRetriesDuration = Duration.ofSeconds(10);

    ConnectionProvider connectionProvider = new PooledConnectionProvider(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), new ConnectionPoolConfig());

    UnifiedJedis client = new UnifiedJedis(connectionProvider, maxAttempts,
        maxTotalRetriesDuration);
    String keyName = "counter";
    client.set(keyName, "0");
    assertEquals("0", client.get(keyName));

    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpoint.getBdbId());

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering DMC restart");
      actionResponse = faultClient.triggerAction("dmc_restart", params);
    } catch (IOException e) {
      fail("Fault Injection Server error:" + e.getMessage());
    }

    log.info("Action id: {}", actionResponse.getActionId());

    int checkEachSeconds = 5;
    int keepExecutingForSeconds = 60;
    int timeoutSeconds = 120;
    long commandsExecuted = 0;

    while (!actionResponse.isCompleted(Duration.ofSeconds(checkEachSeconds),
        Duration.ofSeconds(keepExecutingForSeconds), Duration.ofSeconds(timeoutSeconds))) {
      assertTrue (client.incr(keyName) > 0);
      commandsExecuted++;
    }

    log.info("Commands executed: {}", commandsExecuted);
    log.info("Test took {} seconds",
        Duration.between(actionResponse.getFirstRequestAt(), Instant.now()).getSeconds());

    assertEquals(commandsExecuted, Long.parseLong(client.get(keyName)));

    client.close();
  }

}
