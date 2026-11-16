package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandMetadata;
import redis.clients.jedis.CommandMetadataResolver;
import redis.clients.jedis.CommandMetadataResolverFactory;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisCacheException;

public class CacheFactoryMetadataResolverTest {

  private static final String CUSTOM_METADATA_JSON = "{\n" //
      + "  \"commands\": {\n" //
      + "    \"GET\": {\"flags\": [\"write\"], \"tips\": [], \"firstKey\": 1, \"lastKey\": 1,\n" //
      + "      \"step\": 1, \"keySpecs\": [{\"flags\": [\"rw\"]}]},\n" //
      + "    \"MGET\": {\"flags\": [\"readonly\", \"fast\"], \"tips\": [], \"firstKey\": 1,\n" //
      + "      \"lastKey\": -1, \"step\": 1, \"keySpecs\": [{\"flags\": [\"ro\"]}]}\n" //
      + "  }\n" //
      + "}\n";

  @TempDir
  Path tempDir;

  private CommandMetadataResolver customResolver() throws IOException {
    Path file = tempDir.resolve("custom-metadata.json");
    Files.write(file, CUSTOM_METADATA_JSON.getBytes(StandardCharsets.UTF_8));
    return CommandMetadataResolverFactory.fromFile(file);
  }

  private static CacheKey<String> cacheKey(ProtocolCommand command) {
    return new CacheKey<>(
        new CommandObject<>(new CommandArguments(command).key("k"), BuilderFactory.STRING));
  }

  @Test
  public void resolverDrivesCacheabilityVerdicts() throws IOException {
    Cache cache = new CacheFactory().createCache(CacheConfig.builder().build(), customResolver());

    assertFalse(cache.isCacheable(cacheKey(Command.GET))); // write in the custom metadata
    assertTrue(cache.isCacheable(cacheKey(Command.MGET))); // cacheable read in the custom metadata
    assertFalse(cache.isCacheable(cacheKey(Command.SET))); // unknown: fails closed
  }

  @Test
  public void defaultCacheIsUnaffectedByTheCustomResolver() throws IOException {
    CacheConfig config = CacheConfig.builder().build();
    Cache withResolver = new CacheFactory().createCache(config, customResolver());
    Cache withDefaults = new CacheFactory().createCache(config,
      CommandMetadataResolverFactory.defaultResolver());

    assertFalse(withResolver.isCacheable(cacheKey(Command.GET)));
    assertTrue(withDefaults.isCacheable(cacheKey(Command.GET)));
  }

  @Test
  public void excludedCommandsStillNarrowTheResolverVerdicts() throws IOException {
    CacheConfig config = CacheConfig.builder()
        .excludeCommands(Collections.singleton(Command.MGET)).build();
    Cache cache = new CacheFactory().createCache(config, customResolver());

    assertFalse(cache.isCacheable(cacheKey(Command.MGET))); // cacheable by metadata, excluded
  }

  @Test
  public void metadataFallbackSuppliesMetadataAndTheRulesStillDecide() throws IOException {
    CommandMetadataResolver resolver = customResolver()
        .withFallback(CommandMetadataResolverFactory.defaultResolver());
    Cache cache = new CacheFactory().createCache(CacheConfig.builder().build(), resolver);

    // unknown to the custom metadata: the fallback supplies metadata, the rules judge it
    assertTrue(cache.isCacheable(cacheKey(Command.HGET))); // readonly, keyed
    assertFalse(cache.isCacheable(cacheKey(Command.SET))); // a write stays not cacheable
    // known to the custom metadata: its record wins over the fallback's
    assertFalse(cache.isCacheable(cacheKey(Command.GET)));
  }

  @Test
  public void overrideRecordsCustomizeTheEligibilityDecision() {
    CommandMetadataResolver defaults = CommandMetadataResolverFactory.defaultResolver();
    CommandMetadata getDenied = defaults.resolve("GET").toBuilder().tips("dont_cache").build();
    CommandMetadataResolver resolver = CommandMetadataResolverFactory.of(getDenied)
        .withFallback(defaults);
    Cache cache = new CacheFactory().createCache(CacheConfig.builder().build(), resolver);

    assertFalse(cache.isCacheable(cacheKey(Command.GET))); // corrected by the override record
    assertTrue(cache.isCacheable(cacheKey(Command.HGET))); // untouched default metadata
  }

  @Test
  public void overrideRecordCannotBypassTheEligibilityRules() {
    CommandMetadata writeWithKey = CommandMetadata.builder("SET").flags("write")
        .keyPositions(1, 1, 1).build();
    CommandMetadata readWithKey = CommandMetadata.builder("GET").flags("readonly")
        .keyPositions(1, 1, 1).build();
    Cache cache = new CacheFactory().createCache(CacheConfig.builder().build(), CommandMetadataResolverFactory.of(writeWithKey, readWithKey));

    assertFalse(cache.isCacheable(cacheKey(Command.SET)));
    assertTrue(cache.isCacheable(cacheKey(Command.GET)));
  }

  @Test
  public void customFactoryReceivesTheConfigBoundToTheResolver() throws IOException {
    AtomicReference<CacheConfig> seen = new AtomicReference<>();
    CacheFactory factory = new CacheFactory() {
      @Override
      public Cache createCache(CacheConfig config, CommandMetadataResolver metadataResolver) {
        seen.set(config);
        return super.createCache(config, metadataResolver);
      }
    };
    CacheConfig config = CacheConfig.builder().maxSize(7).build();
    Cache cache = factory.createCache(config, customResolver());

    assertFalse(cache.isCacheable(cacheKey(Command.GET))); // write in the custom metadata
    assertTrue(cache.isCacheable(cacheKey(Command.MGET)));
    assertTrue(seen.get().getMaxSize() == 7);
    // the config itself keeps the policy over the default metadata
    assertTrue(config.getCacheable().isCacheable(Command.GET, Collections.emptyList()));
  }

  @Test
  public void customCacheableStillWinsOverTheResolver() throws IOException {
    Cacheable cacheNothing = (command, keys) -> false;
    CacheConfig config = CacheConfig.builder().cacheable(cacheNothing).build();
    Cache cache = new CacheFactory().createCache(config, customResolver());

    // current CustomCacheablePolicy behavior: the user-provided Cacheable decides alone
    assertFalse(cache.isCacheable(cacheKey(Command.MGET)));
  }

  @Test
  public void customCacheClassReceivesTheResolverDerivedCacheable() throws IOException {
    CacheConfig config = CacheConfig.builder().cacheClass(TestCache.class).build();
    Cache cache = new CacheFactory().createCache(config, customResolver());

    assertTrue(cache instanceof TestCache);
    assertFalse(cache.isCacheable(cacheKey(Command.GET))); // write in the custom metadata
    assertTrue(cache.isCacheable(cacheKey(Command.MGET)));
  }

  @Test
  public void nullResolverIsRejected() {
    CacheConfig config = CacheConfig.builder().build();
    assertThrows(IllegalArgumentException.class, () -> new CacheFactory().createCache(config, null));
  }

  @Test
  public void rawConfigWithoutBuilderIsRejected() throws IOException {
    CommandMetadataResolver resolver = customResolver();
    assertThrows(JedisCacheException.class, () -> new CacheFactory().createCache(new CacheConfig(), resolver));
  }

  private static CacheKey<String> cacheKey(ProtocolCommand command, String key) {
    return new CacheKey<>(
        new CommandObject<>(new CommandArguments(command).key(key), BuilderFactory.STRING));
  }

  @Test
  public void narrowingCacheableNarrowsEligibleInvocations() {
    CacheConfig config = CacheConfig.builder()
        .excludeCommands((command, keys) -> !keys.contains("session:1")).build();
    Cache cache = new CacheFactory().createCache(config,
      CommandMetadataResolverFactory.defaultResolver());

    assertTrue(cache.isCacheable(cacheKey(Command.GET, "user:1")));
    assertFalse(cache.isCacheable(cacheKey(Command.GET, "session:1")));
  }

  @Test
  public void narrowingCacheableCannotWidenAndIsNotAskedAboutIneligibleCommands() {
    AtomicReference<ProtocolCommand> asked = new AtomicReference<>();
    CacheConfig config = CacheConfig.builder().excludeCommands((command, keys) -> {
      asked.set(command);
      return true;
    }).build();
    Cache cache = new CacheFactory().createCache(config,
      CommandMetadataResolverFactory.defaultResolver());

    assertFalse(cache.isCacheable(cacheKey(Command.SET))); // a write stays not cacheable
    assertTrue(asked.get() == null);
  }

  @Test
  public void narrowingCacheableIsKeptForAClientLevelResolverAndCombinesWithExcludedCommands()
      throws IOException {
    CacheConfig config = CacheConfig.builder()
        .excludeCommands(Collections.singleton(Command.HGET))
        .excludeCommands((command, keys) -> !keys.contains("session:1")).build();
    Cache cache = new CacheFactory().createCache(config,
      customResolver().withFallback(CommandMetadataResolverFactory.defaultResolver()));

    assertTrue(cache.isCacheable(cacheKey(Command.MGET, "user:1")));
    assertFalse(cache.isCacheable(cacheKey(Command.MGET, "session:1")));
    assertFalse(cache.isCacheable(cacheKey(Command.HGET, "user:1")));
  }

  @Test
  public void narrowingCacheableCannotBeCombinedWithACustomCacheable() {
    assertThrows(IllegalArgumentException.class, () -> CacheConfig.builder()
        .cacheable((command, keys) -> true).excludeCommands((command, keys) -> true).build());
  }
}
