package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.MetadataResolver;
import redis.clients.jedis.MetadataResolver.CommandMetadata;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit tests for the eligibility rules, rule by rule, and for the verdict precedence: overrides,
 * then known server metadata fixes, then resolved metadata.
 */
public class CacheabilityResolverTest {

  private static CommandMetadata metadata(String flags, String tips, long firstKey, long step,
      boolean hasKeyNameSpec) {
    return redis.clients.jedis.CommandMetadataTestFactory.create("X", toSet(flags), toSet(tips),
      firstKey, -1, step, hasKeyNameSpec);
  }

  private static java.util.Set<String> toSet(String spaceSeparated) {
    return spaceSeparated.isEmpty() ? Collections.emptySet()
        : new java.util.HashSet<>(java.util.Arrays.asList(spaceSeparated.split(" ")));
  }

  @Test
  public void eligibleCommandIsCacheable() {
    assertTrue(
      CacheabilityResolver.isClientSideCacheable(metadata("fast readonly", "", 1, 1, true)));
  }

  @Test
  public void dontCacheTipWinsOverEverything() {
    assertFalse(CacheabilityResolver
        .isClientSideCacheable(metadata("fast readonly", "dont_cache", 1, 1, true)));
  }

  @Test
  public void nonReadonlyIsNotCacheable() {
    assertFalse(
      CacheabilityResolver.isClientSideCacheable(metadata("write denyoom", "", 1, 1, true)));
  }

  @Test
  public void blockingIsNotCacheable() {
    assertFalse(
      CacheabilityResolver.isClientSideCacheable(metadata("readonly blocking", "", 1, 1, true)));
  }

  @Test
  public void keylessIsNotCacheable() {
    assertFalse(CacheabilityResolver.isClientSideCacheable(metadata("readonly", "", 0, 0, false)));
  }

  @Test
  public void legacyKeyMetadataProvesKeyArgument() {
    assertTrue(CacheabilityResolver.isClientSideCacheable(metadata("readonly", "", 1, 1, false)));
  }

  @Test
  public void keyNameSpecProvesKeyArgumentWithoutLegacyMetadata() {
    // movablekeys commands report firstKey == 0 but still name keys via key specs
    assertTrue(
      CacheabilityResolver.isClientSideCacheable(metadata("readonly movablekeys", "", 0, 0, true)));
  }

  @Test
  public void nondeterministicOutputIsNotCacheable() {
    assertFalse(CacheabilityResolver
        .isClientSideCacheable(metadata("readonly", "nondeterministic_output", 1, 1, true)));
  }

  @Test
  public void scriptRunnerIsNotCacheable() {
    assertFalse(CacheabilityResolver
        .isClientSideCacheable(metadata("readonly script_runner", "", 0, 0, true)));
  }

  /** Well-known alignment cases, resolved over the real generated metadata table. */
  @Test
  public void alignmentCasesOverGeneratedMetadata() {
    Map<ProtocolCommand, Boolean> cacheabilityMap = new CacheabilityResolver(new MetadataResolver())
        .resolve();
    assertEquals(true, cacheabilityMap.get(Command.GET));
    assertEquals(false, cacheabilityMap.get(Command.KEYS)); // no key-name argument
    assertEquals(false, cacheabilityMap.get(Command.XPENDING)); // nondeterministic_output
    assertEquals(false, cacheabilityMap.get(Command.EVAL_RO)); // script_runner
    assertEquals(false, cacheabilityMap.get(Command.EVALSHA_RO)); // script_runner
    assertEquals(false, cacheabilityMap.get(Command.FCALL_RO)); // script_runner
    assertEquals(false, cacheabilityMap.get(Command.XREADGROUP)); // not readonly
    assertEquals(false, cacheabilityMap.get(Command.XREAD)); // blocking
  }

  /** Server metadata says TOUCH and VRANDMEMBER are cacheable; the built-in fixes deny them. */
  @Test
  public void knownServerMetadataFixesWinOverMetadata() {
    Map<ProtocolCommand, Boolean> cacheabilityMap = new CacheabilityResolver(new MetadataResolver())
        .resolve();
    assertEquals(false, cacheabilityMap.get(Command.TOUCH));
    assertEquals(false, cacheabilityMap.get(Command.VRANDMEMBER));
  }

  @Test
  public void overridesWinOverFixesAndMetadata() {
    Set<ProtocolCommand> overrides = new HashSet<>();
    overrides.add(Command.TOUCH); // already denied by the metadata fix; stays denied
    overrides.add(Command.GET); // excluded despite a cacheable metadata verdict
    Cacheable cacheable = new CacheabilityResolver(new MetadataResolver(), overrides, null);

    assertFalse(cacheable.isCacheable(Command.TOUCH, Collections.emptyList()));
    assertFalse(cacheable.isCacheable(Command.GET, Collections.emptyList()));
    // commands not overridden keep their metadata verdict
    assertTrue(cacheable.isCacheable(Command.MGET, Collections.emptyList()));
  }

  /** Commands without metadata get no verdict, so lookups on them fail closed. */
  @Test
  public void commandsWithoutMetadataGetNoVerdict() {
    Map<ProtocolCommand, Boolean> cacheabilityMap = new CacheabilityResolver(new MetadataResolver())
        .resolve();
    assertFalse(cacheabilityMap.containsKey(Command.SENTINEL));
  }

  /** Commands with no verdict fail closed at lookup time. */
  @Test
  public void unknownCommandFailsClosed() {
    Cacheable cacheable = new CacheabilityResolver(new MetadataResolver());
    ProtocolCommand unknown = () -> SafeEncoder.encode("NO.SUCH.COMMAND");
    assertFalse(cacheable.isCacheable(unknown, Collections.emptyList()));
    // and again, exercising the warn-once path
    assertFalse(cacheable.isCacheable(unknown, Collections.emptyList()));
  }

  /** A cacheable command whose name contains the pipe character is evaluated as eligible. */
  @Test
  public void pipeNamedCacheableCommandIsEligible() {
    MetadataResolver metadataResolver = new MetadataResolver();
    // eligible by the metadata rules, resolved by their full PARENT|CHILD name
    assertTrue(
      CacheabilityResolver.isClientSideCacheable(metadataResolver.resolve("MEMORY|USAGE")));
    assertTrue(
      CacheabilityResolver.isClientSideCacheable(metadataResolver.resolve("XINFO|GROUPS")));

    // and through the Cacheable interface, with a fallback binding the name to its metadata
    Cacheable resolver = new CacheabilityResolver(metadataResolver, null,
        (command, keys) -> CacheabilityResolver
            .isClientSideCacheable(metadataResolver.resolve(SafeEncoder.encode(command.getRaw()))));
    assertTrue(
      resolver.isCacheable(() -> SafeEncoder.encode("MEMORY|USAGE"), Collections.emptyList()));
  }

  /**
   * Container subcommand names contain a pipe ({@code PARENT|CHILD}): the metadata resolves by full
   * name and the eligibility rules judge it, while a {@link ProtocolCommand} carrying a pipe name
   * has no verdict in the map and follows the fallback / fail-closed path.
   */
  @Test
  public void pipeSeparatedSubcommandNames() {
    MetadataResolver metadataResolver = new MetadataResolver();
    assertTrue(
      CacheabilityResolver.isClientSideCacheable(metadataResolver.resolve("XINFO|STREAM")));
    assertTrue(
      CacheabilityResolver.isClientSideCacheable(metadataResolver.resolve("MEMORY|USAGE")));
    // nondeterministic_output sibling stays ineligible
    assertFalse(
      CacheabilityResolver.isClientSideCacheable(metadataResolver.resolve("XINFO|CONSUMERS")));

    Cacheable resolver = new CacheabilityResolver(metadataResolver);
    // the merged command from the subcommand constructor hits its precomputed verdict
    assertTrue(resolver.isCacheable(new CommandObject<>(new CommandArguments(Command.XINFO,
        redis.clients.jedis.Protocol.Keyword.STREAM).key("k"),BuilderFactory.STRING)));
    // a foreign ProtocolCommand with a pipe name is not value-equal to the map keys: fail closed
    ProtocolCommand pipeCommand = () -> SafeEncoder.encode("XINFO|STREAM");
    assertFalse(resolver.isCacheable(pipeCommand, Collections.emptyList()));
  }

  private static CacheKey<?> cacheKey(redis.clients.jedis.CommandArguments args) {
    return new CacheKey<>(
        new CommandObject<>(args, redis.clients.jedis.BuilderFactory.STRING));
  }

  private static boolean isCacheable(Cacheable cacheable, CacheKey<?> cacheKey) {
    // what AbstractCache does: the CacheKey exposes the pipe-merged full command
    return cacheable.isCacheable(cacheKey.getCommandObject());
  }

  /** Container subcommands declared at construction are judged by their own metadata. */
  @Test
  public void containerSubcommandsResolveViaFullCommand() {
    CacheabilityResolver resolver = new CacheabilityResolver(new MetadataResolver());
    redis.clients.jedis.Protocol.Keyword stream = redis.clients.jedis.Protocol.Keyword.STREAM;
    redis.clients.jedis.Protocol.Keyword groups = redis.clients.jedis.Protocol.Keyword.GROUPS;
    redis.clients.jedis.Protocol.Keyword consumers = redis.clients.jedis.Protocol.Keyword.CONSUMERS;
    redis.clients.jedis.Protocol.Keyword usage = redis.clients.jedis.Protocol.Keyword.USAGE;

    assertTrue(isCacheable(resolver,
      cacheKey(new CommandArguments(Command.XINFO, stream).key("k"))));
    assertTrue(isCacheable(resolver,
      cacheKey(new CommandArguments(Command.XINFO, groups).key("k"))));
    assertTrue(isCacheable(resolver,
      cacheKey(new CommandArguments(Command.MEMORY, usage).key("k"))));
    // nondeterministic sibling stays denied
    assertFalse(isCacheable(resolver, cacheKey(
      new CommandArguments(Command.XINFO, consumers).key("k").add("g"))));
    // and the cached-verdict path returns the same answer
    assertTrue(isCacheable(resolver,
      cacheKey(new CommandArguments(Command.XINFO, stream).key("k"))));
  }

  /**
   * Exclusions apply to the exact command: excluding a container parent does not exclude its
   * subcommands; a subcommand is excluded individually by its PARENT|CHILD name.
   */
  @Test
  public void exclusionsApplyToExactCommandOnly() {
    CacheabilityResolver parentExcluded = new CacheabilityResolver(new MetadataResolver(),
        Collections.singleton(Command.XINFO), null);
    assertTrue(isCacheable(parentExcluded, cacheKey(new CommandArguments(Command.XINFO,
        redis.clients.jedis.Protocol.Keyword.STREAM).key("k"))));

    ProtocolCommand xinfoStream = () -> SafeEncoder.encode("XINFO|STREAM");
    CacheabilityResolver subcommandExcluded = new CacheabilityResolver(new MetadataResolver(),
        Collections.singleton(xinfoStream), null);
    assertFalse(isCacheable(subcommandExcluded, cacheKey(new CommandArguments(Command.XINFO,
        redis.clients.jedis.Protocol.Keyword.STREAM).key("k"))));
    // the sibling subcommand keeps its verdict
    assertTrue(isCacheable(subcommandExcluded, cacheKey(new CommandArguments(Command.XINFO,
        redis.clients.jedis.Protocol.Keyword.GROUPS).key("k"))));
  }

  /** Commands without a declared subcommand keep the fast per-command verdict. */
  @Test
  public void nonContainerCommandsKeepVerdictThroughCacheKeyPath() {
    CacheabilityResolver resolver = new CacheabilityResolver(new MetadataResolver());
    assertTrue(isCacheable(resolver,
      cacheKey(new CommandArguments(Command.GET).key("k"))));
    assertFalse(isCacheable(resolver,
      cacheKey(new CommandArguments(Command.SET).key("k").add("v"))));
  }

  /** Exclusions normalize to the canonical map key, so custom ProtocolCommand instances work. */
  @Test
  public void exclusionsWorkForCustomProtocolCommandInstances() {
    ProtocolCommand rawGet = () -> SafeEncoder.encode("GET");
    Cacheable cacheable = new CacheabilityResolver(new MetadataResolver(),
        Collections.singleton(rawGet), null);
    assertFalse(cacheable.isCacheable(Command.GET, Collections.emptyList()));
    // other commands keep their metadata verdict
    assertTrue(cacheable.isCacheable(Command.MGET, Collections.emptyList()));
  }

  /** SORT_RO replies depend on BY/GET pattern keys that invalidation cannot track. */
  @Test
  public void sortRoDeniedByMetadataFix() {
    Cacheable resolver = new CacheabilityResolver(new MetadataResolver());
    assertFalse(resolver.isCacheable(Command.SORT_RO, Collections.emptyList()));
  }

  @Test
  public void nullMetadataResolverRejected() {
    assertThrows(NullPointerException.class, () -> new CacheabilityResolver(null));
    // null overrides means "no overrides" and behaves like the default policy
    Cacheable cacheable = new CacheabilityResolver(new MetadataResolver(), null, null);
    assertTrue(cacheable.isCacheable(Command.GET, Collections.emptyList()));
  }
}
