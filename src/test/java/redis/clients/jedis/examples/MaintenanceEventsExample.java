package redis.clients.jedis.examples;

import java.time.Duration;
import java.util.Date;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.TimeoutOptions;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Manual example for server maintenance notifications. A {@link RedisClient} is opened with
 * maintenance enabled (RESP3 + {@code CLIENT MAINT_NOTIFICATIONS ON}) and runs a small workload for
 * a while. Trigger a maintenance operation on the server (e.g. a shard migration or failover) during
 * the run; the example logs whenever a connection's timeouts are relaxed in response to a
 * notification. Observation borrows one connection from the client's pool via
 * {@link RedisClient#getPool()}.
 * <p>
 * Configure via system properties: {@code redis.host} (127.0.0.1), {@code redis.port} (6379),
 * {@code redis.username}, {@code redis.password}, {@code example.durationSeconds} (300). Enable
 * DEBUG logging on {@code redis.clients.jedis.MaintenanceEventController} to also see MOVING rebinds.
 */
public class MaintenanceEventsExample {

  public static void main(String[] args) throws InterruptedException {
    HostAndPort hostAndPort = new HostAndPort(System.getProperty("redis.host", "127.0.0.1"),
        Integer.getInteger("redis.port", 6379));
    int durationSeconds = Integer.getInteger("example.durationSeconds", 300);

    MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED)
        .timeoutOptions(
          TimeoutOptions.builder().proactiveTimeoutsRelaxing(Duration.ofSeconds(20)).build())
        .build();

    DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3).socketTimeoutMillis(5000)
        .maintNotificationsConfig(maintConfig);
    if (System.getProperty("redis.username") != null) {
      configBuilder.user(System.getProperty("redis.username"));
    }
    if (System.getProperty("redis.password") != null) {
      configBuilder.password(System.getProperty("redis.password"));
    }
    JedisClientConfig clientConfig = configBuilder.build();

    System.out.printf("Connecting to %s with maintenance notifications ENABLED (RESP3).%n",
      hostAndPort);
    System.out.printf("Running for %ds - trigger a maintenance operation on the server now.%n",
      durationSeconds);

    long deadline = System.currentTimeMillis() + durationSeconds * 1000L;
    try (RedisClient client = RedisClient.builder().hostAndPort(hostAndPort)
        .clientConfig(clientConfig).build()) {

      String key = "maintenance-example:counter";
      client.set(key, "0");

      // Observe relaxation on one borrowed connection while the workload runs on the client.
      Connection observer = client.getPool().getResource();
      boolean relaxed = false;
      try {
        while (System.currentTimeMillis() < deadline) {
          try {
            client.incr(key); // application workload via RedisClient
            observer.ping(); // reads and processes any pending maintenance push
            boolean nowRelaxed = observer.isRelaxedTimeoutActive();
            if (nowRelaxed != relaxed) {
              relaxed = nowRelaxed;
              System.out.printf("[%tT] relaxed timeout %s%n", new Date(),
                relaxed ? "ACTIVE - maintenance event received and applied"
                    : "reverted - maintenance window ended");
            }
          } catch (JedisException e) {
            System.out.printf("[%tT] command failed (%s); reconnecting observer%n", new Date(),
              e.getMessage());
            observer.close();
            observer = client.getPool().getResource();
            relaxed = observer.isRelaxedTimeoutActive();
          }
          Thread.sleep(500);
        }
      } finally {
        observer.close();
      }
    }
    System.out.println("Example finished.");
  }
}
