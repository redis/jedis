package redis.clients.jedis.scenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class FakeApp implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FakeApp.class);

  private FaultInjectionClient.TriggerActionResponse actionResponse = null;
  private final UnifiedJedis client;
  private final ExecutedAction action;
  private List<JedisException> exceptions = new ArrayList<>();

  @FunctionalInterface
  public interface ExecutedAction {
      boolean run(UnifiedJedis client);
  }

  public FakeApp(UnifiedJedis client, ExecutedAction action) {
    this.client = client;
    this.action = action;
  }

  public void setAction(FaultInjectionClient.TriggerActionResponse actionResponse) {
    this.actionResponse = actionResponse;
  }

  public List<JedisException> capturedExceptions() {
    return exceptions;
  }

  public void run() {
    log.info("Starting FakeApp");

    int checkEachSeconds = 5;
    int keepExecutingForSeconds = 60;
    int timeoutSeconds = 120;

    while (actionResponse == null || !actionResponse.isCompleted(
        Duration.ofSeconds(checkEachSeconds), Duration.ofSeconds(keepExecutingForSeconds),
        Duration.ofSeconds(timeoutSeconds))) {
      try {
        boolean success = action.run(client);

        if (!success)
          break;
      } catch (JedisConnectionException e) {
        log.error("Error executing action", e);
        exceptions.add(e);
      }
    }
  }
}
