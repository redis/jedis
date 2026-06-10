package redis.clients.jedis.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.UnifiedJedis;

/**
 * Helper for toggling server-side Redis Search feature flags around an integration test class.
 * <p>
 * Several COLLECT-style features in Redis Search are gated behind
 * {@code search-enable-unstable-features}; the server replies
 * {@code SEARCH_QUERY_BAD `COLLECT` is unavailable when `ENABLE_UNSTABLE_FEATURES` is off} when the
 * flag is off. This helper flips it on through the provided {@link UnifiedJedis} client.
 * <p>
 * For {@link redis.clients.jedis.RedisClusterClient} the {@code CONFIG SET} is automatically
 * broadcast to every primary shard by the cluster executor, so a single call here is enough — no
 * per-node iteration is required.
 */
public final class SearchFeatureFlags {

  public static final String UNSTABLE_FEATURES_KEY = "search-enable-unstable-features";

  private static final Logger log = LoggerFactory.getLogger(SearchFeatureFlags.class);

  private static final int VERIFY_ATTEMPTS = 3;
  private static final long VERIFY_BACKOFF_MILLIS = 100L;

  private SearchFeatureFlags() {
  }

  /**
   * Set {@code search-enable-unstable-features=yes} via the supplied client and return the previous
   * value so callers can restore it later. Returns {@code null} when the flag is not configurable
   * on the target Redis build — the caller should typically assume-skip in that case.
   */
  public static String enableUnstable(UnifiedJedis client) {
    return set(client, "yes");
  }

  /**
   * Sets {@code search-enable-unstable-features} to {@code value} via the supplied client and
   * verifies (with up to {@value #VERIFY_ATTEMPTS} retries) that the new value is observable
   * through a follow-up {@code CONFIG GET}. The verification step matters for
   * {@link redis.clients.jedis.RedisClusterClient} because the broadcast {@code CONFIG SET}
   * propagates per-node and we want to be sure every shard has caught up before tests start running
   * queries that depend on the flag.
   * <p>
   * Returns the previous value (or {@code null} when the flag isn't configurable, the call failed,
   * or verification did not converge within the retry budget).
   */
  public static String set(UnifiedJedis client, String value) {
    String previous = readFlag(client);
    if (previous == null) {
      return null;
    }
    if (value.equalsIgnoreCase(previous)) {
      return previous;
    }

    try {
      client.configSet(UNSTABLE_FEATURES_KEY, value);
    } catch (Exception e) {
      log.debug("CONFIG SET {}={} failed", UNSTABLE_FEATURES_KEY, value, e);
      return null;
    }

    for (int attempt = 1; attempt <= VERIFY_ATTEMPTS; attempt++) {
      String observed = readFlag(client);
      if (observed != null && value.equalsIgnoreCase(observed)) {
        return previous;
      }
      log.debug("attempt {}/{} — {} still reads as {}", attempt, VERIFY_ATTEMPTS,
        UNSTABLE_FEATURES_KEY, observed);
      try {
        Thread.sleep(VERIFY_BACKOFF_MILLIS);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return null;
      }

    }
    log.warn("CONFIG SET {}={} did not converge after {} retries", UNSTABLE_FEATURES_KEY, value,
      VERIFY_ATTEMPTS);
    return null;
  }

  private static String readFlag(UnifiedJedis client) {
    try {
      Map<String, String> reply = client.configGet(UNSTABLE_FEATURES_KEY);
      return reply == null ? null : reply.get(UNSTABLE_FEATURES_KEY);
    } catch (Exception e) {
      log.debug("CONFIG GET {} failed", UNSTABLE_FEATURES_KEY, e);
      return null;
    }
  }
}
