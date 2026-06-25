package redis.clients.jedis.csc;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisClient;

/**
 * Integration tests that exercise BCAST tracking mode end-to-end against a real Redis 7.4+
 * standalone server.
 *
 * <p>The behaviors under test:
 * <ul>
 *   <li>A modification to a key matching one of the configured prefixes invalidates the cache
 *       entry.</li>
 *   <li>A modification to a key that does <i>not</i> match any prefix does <b>not</b> trigger
 *       an invalidation. In BCAST mode the server only broadcasts for matching prefixes.</li>
 * </ul>
 */
public class BroadcastTrackingIntegrationTest extends ClientSideCacheTestBase {

  @Test
  public void invalidationFiresForPrefixedKey() {
    CacheConfig cfg = CacheConfig.builder()
        .bcast()
        .prefixes("user:")
        .build();

    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(cfg)
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      Cache cache = jedis.getCache();

      control.set("user:1", "alice");
      assertEquals("alice", jedis.get("user:1"));
      assertEquals(1, cache.getSize());

      // Mutate the prefixed key from another connection — server should broadcast invalidate.
      control.set("user:1", "bob");

      await().atMost(Duration.ofSeconds(5))
          .until(() -> cache.getSize() == 0);
    }
  }

  @Test
  public void noInvalidationForNonPrefixedKey() {
    CacheConfig cfg = CacheConfig.builder()
        .bcast()
        .prefixes("user:")
        .build();

    try (RedisClient jedis = RedisClient.builder()
        .hostAndPort(hnp)
        .clientConfig(clientConfig.get())
        .cacheConfig(cfg)
        .poolConfig(singleConnectionPoolConfig.get())
        .build()) {
      Cache cache = jedis.getCache();

      control.set("other:1", "x");
      assertEquals("x", jedis.get("other:1"));
      assertEquals(1, cache.getSize());

      // Mutate a non-prefixed key. In BCAST mode with PREFIX user: the server must NOT
      // send an invalidation for this key. Cache size should remain stable.
      control.set("other:1", "y");

      // Give the server a moment to deliver any (unexpected) push messages.
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Trigger any pending pushes by issuing a no-op command.
      jedis.ping();
      assertEquals(1, cache.getSize(), "Non-prefixed key changes must not invalidate the cache");
    }
  }
}
