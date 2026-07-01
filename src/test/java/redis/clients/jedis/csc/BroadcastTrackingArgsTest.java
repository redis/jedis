package redis.clients.jedis.csc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the BCAST / NOLOOP tracking-mode wiring. These exercise
 * {@link CacheConfig}, {@link DefaultCache}, and the package-private
 * {@link CacheConnection#buildTrackingArgs(Cache)} helper without requiring a running Redis
 * instance.
 */
public class BroadcastTrackingArgsTest {

  @Test
  public void configDefaultsAreNonBroadcast() {
    CacheConfig cfg = CacheConfig.builder().build();
    assertFalse(cfg.isBroadcastMode());
    assertEquals(Collections.emptyList(), cfg.getPrefixes());
    assertFalse(cfg.noloop());
  }

  @Test
  public void configBuilderStoresBroadcastAndPrefixes() {
    CacheConfig cfg = CacheConfig.builder()
        .bcast()
        .prefixes("user:", "order:")
        .build();
    assertTrue(cfg.isBroadcastMode());
    assertEquals(Arrays.asList("user:", "order:"), cfg.getPrefixes());
  }

  @Test
  public void configPrefixesNullCoercedToEmptyList() {
    CacheConfig cfg = CacheConfig.builder()
        .bcast()
        .prefixes((List<String>) null)
        .build();
    assertEquals(Collections.emptyList(), cfg.getPrefixes());
  }

  @Test
  public void cacheReflectsConfigViaFactory() {
    CacheConfig cfg = CacheConfig.builder()
        .bcast()
        .prefixes("user:")
        .build();
    Cache cache = CacheFactory.getCache(cfg);
    assertTrue(cache.isBroadcastMode());
    assertEquals(Collections.singletonList("user:"), cache.getPrefixes());
  }
}
