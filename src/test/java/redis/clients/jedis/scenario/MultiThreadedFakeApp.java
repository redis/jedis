package redis.clients.jedis.scenario;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiThreadedFakeApp extends FakeApp {

  private final ExecutorService executorService;
  private final RateLimiter rateLimiter;

  public MultiThreadedFakeApp(UnifiedJedis client, FakeApp.ExecutedAction action, int numThreads) {
    this(client, action, numThreads, null);
  }

  public MultiThreadedFakeApp(UnifiedJedis client, FakeApp.ExecutedAction action, int numThreads, RateLimiterConfig config) {
    super(client, action);
    this.executorService = Executors.newFixedThreadPool(numThreads);

    if (config != null) {
      this.rateLimiter = RateLimiterRegistry.of(config).rateLimiter("fakeAppLimiter");
    } else {
      this.rateLimiter = null;
    }
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
        if (rateLimiter != null) {
          RateLimiter.waitForPermission(rateLimiter);
        }
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
