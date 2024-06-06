package redis.clients.jedis.scenario;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class RetryingTest {

  private static final Logger log = LoggerFactory.getLogger(RetryingTest.class);

  private final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("re-standalone");

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @Parameterized.Parameters
  public static Iterable<?> data() {
      return Arrays.asList(/*"dmc_restart",*/ "network_failure");
  }

  @Parameterized.Parameter
  public String triggerAction;

  @Test
  public void testWithPool() {
    ConnectionProvider connectionProvider = new PooledConnectionProvider(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), RecommendedSettings.poolConfig);

    UnifiedJedis client = new UnifiedJedis(connectionProvider, RecommendedSettings.MAX_RETRIES,
        RecommendedSettings.MAX_TOTAL_RETRIES_DURATION);
    String keyName = "counter";
    client.set(keyName, "0");
    assertEquals("0", client.get(keyName));

    AtomicLong commandsExecuted = new AtomicLong();

    // Start thread that imitates an application that uses the client
    FakeApp fakeApp = new FakeApp(client, (UnifiedJedis c) -> {
      assertTrue(client.incr(keyName) > 0);
      commandsExecuted.getAndIncrement();
      return true;
    });
    Thread t = new Thread(fakeApp);
    t.start();

    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpoint.getBdbId());

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering {}", triggerAction);
      actionResponse = faultClient.triggerAction(triggerAction, params);
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

    log.info("Commands executed: {}", commandsExecuted.get());
    assertEquals(commandsExecuted.get(), Long.parseLong(client.get(keyName)));
    assertTrue(fakeApp.capturedExceptions().isEmpty());

    client.close();
  }

}
