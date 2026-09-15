package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.exceptions.JedisCacheException;

/**
 * Guards which constructor {@link CacheFactory} selects for a custom cache class, and that the
 * policy handed over reflects the {@link CacheConfig} (default, exclusions, custom, explicit null).
 */
public class CacheFactoryTest {

  /** Custom cache whose two constructors are distinguishable: the two-argument one denies all. */
  public static class RecordingCache extends DefaultCache {

    final int constructorArity;

    public RecordingCache(int maxSize, EvictionPolicy evictionPolicy) {
      super(maxSize, (command, keys) -> false, evictionPolicy);
      this.constructorArity = 2;
    }

    public RecordingCache(int maxSize, EvictionPolicy evictionPolicy, Cacheable cacheable) {
      super(maxSize, cacheable, evictionPolicy);
      this.constructorArity = 3;
    }
  }

  private static final CacheKey<?> GET = cacheKey(new CommandArguments(Command.GET).key("k"));
  private static final CacheKey<?> MGET = cacheKey(new CommandArguments(Command.MGET).key("k"));
  private static final CacheKey<?> XINFO_STREAM = cacheKey(
    new CommandArguments(Command.XINFO, Keyword.STREAM).key("k"));

  private static CacheKey<?> cacheKey(CommandArguments args) {
    return new CacheKey<>(new CommandObject<>(args, BuilderFactory.STRING));
  }

  private static RecordingCache create(CacheConfig.Builder builder) {
    return (RecordingCache) CacheFactory.getCache(builder.cacheClass(RecordingCache.class).build());
  }

  @Test
  public void unsetCacheableUsesThreeArgumentConstructorWithDefaultPolicy() {
    RecordingCache cache = create(CacheConfig.builder());
    assertEquals(3, cache.constructorArity);
    assertTrue(cache.isCacheable(GET));
  }

  @Test
  public void explicitNullCacheableUsesTwoArgumentConstructor() {
    CacheConfig config = CacheConfig.builder().cacheable(null).build();
    assertNull(config.getCacheable());

    RecordingCache cache = create(CacheConfig.builder().cacheable(null));
    assertEquals(2, cache.constructorArity);
    assertFalse(cache.isCacheable(GET));
  }

  @Test
  public void customCacheableIsPassedToThreeArgumentConstructor() {
    Cacheable denyAll = (command, keys) -> false;
    CacheConfig config = CacheConfig.builder().cacheable(denyAll).build();
    assertInstanceOf(CustomCacheablePolicy.class, config.getCacheable());

    RecordingCache cache = create(CacheConfig.builder().cacheable(denyAll));
    assertEquals(3, cache.constructorArity);
    assertFalse(cache.isCacheable(GET));
  }

  /** The deprecated default instance is the default policy, subcommands included. */
  @Test
  public void defaultCacheableInstanceIsTheDefaultPolicy() {
    RecordingCache cache = create(CacheConfig.builder().cacheable(DefaultCacheable.INSTANCE));
    assertEquals(3, cache.constructorArity);
    assertTrue(cache.isCacheable(XINFO_STREAM));
  }

  @Test
  public void exclusionsReachCustomCacheClass() {
    RecordingCache cache = create(
      CacheConfig.builder().excludeCommands(Collections.singleton(Command.GET)));
    assertEquals(3, cache.constructorArity);
    assertFalse(cache.isCacheable(GET));
    assertTrue(cache.isCacheable(MGET));
  }

  @Test
  public void explicitNullCacheableRejectsExclusionsAndFallback() {
    assertThrows(IllegalArgumentException.class, () -> CacheConfig.builder().cacheable(null)
        .excludeCommands(Collections.singleton(Command.GET)).build());
    assertThrows(IllegalArgumentException.class, () -> CacheConfig.builder().cacheable(null)
        .withFallback((command, keys) -> true).build());
  }

  @Test
  public void explicitNullCacheableCannotCreateDefaultCache() {
    assertThrows(JedisCacheException.class,
      () -> CacheFactory.getCache(CacheConfig.builder().cacheable(null).build()));
  }

  @Test
  public void evictionPolicySupplierIsUsedPerCache() {
    EvictionPolicy shared = new LRUEviction(10);
    CacheConfig configured = CacheConfig.builder().evictionPolicy(shared).build();
    assertSame(shared, configured.getEvictionPolicySupplier().get());

    CacheConfig supplied = CacheConfig.builder().evictionPolicy(shared)
        .evictionPolicySupplier(() -> new LRUEviction(10)).build();
    assertTrue(supplied.getEvictionPolicySupplier().get() != shared);
    assertTrue(supplied.getEvictionPolicySupplier().get() != supplied.getEvictionPolicySupplier().get());
  }
}
