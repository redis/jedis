package redis.clients.jedis.scenario;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiThreadedFakeApp extends FakeApp {

  private final ExecutorService executorService;

  public MultiThreadedFakeApp(UnifiedJedis client, FakeApp.ExecutedAction action, int numThreads) {
    super(client, action);
    this.executorService = Executors.newFixedThreadPool(numThreads);
  }

  @Override
  public void run() {
    log.info("Starting FakeApp");

    int checkEachSeconds = 5;
    int timeoutSeconds = 120;

    while (actionResponse == null || !actionResponse.isCompleted(
        Duration.ofSeconds(checkEachSeconds), Duration.ofSeconds(keepExecutingForSeconds),
        Duration.ofSeconds(timeoutSeconds))) {
      try {
        executorService.submit(() -> action.run(client));
      } catch (JedisConnectionException e) {
        log.error("Error executing action", e);
        exceptions.add(e);
      }
    }

    executorService.shutdown();

    try {
      if (!executorService.awaitTermination(keepExecutingForSeconds, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.error("Error waiting for executor service to terminate", e);
    }
  }
}
