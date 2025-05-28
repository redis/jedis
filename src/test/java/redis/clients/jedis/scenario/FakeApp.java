package redis.clients.jedis.scenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class FakeApp implements Runnable {

  protected static final Logger log = LoggerFactory.getLogger(FakeApp.class);

  public void setKeepExecutingForSeconds(int keepExecutingForSeconds) {
    this.keepExecutingForSeconds = keepExecutingForSeconds;
  }

  protected int keepExecutingForSeconds = 60;

  protected FaultInjectionClient.TriggerActionResponse actionResponse = null;
  protected final UnifiedJedis client;
  protected final ExecutedAction action;
  protected List<JedisException> exceptions = new ArrayList<>();

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
    int timeoutSeconds = 120;

    while (actionResponse == null || !actionResponse.isCompleted(
        Duration.ofSeconds(checkEachSeconds), Duration.ofSeconds(keepExecutingForSeconds),
        Duration.ofSeconds(timeoutSeconds))) {
      try {
        boolean success = action.run(client);

        if (!success) break;
      } catch (JedisConnectionException e) {
        log.error("Error executing action", e);
        exceptions.add(e);
      }
    }
  }
}
