package redis.clients.jedis.csc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandMetadata;
import redis.clients.jedis.CommandMetadataResolver;
import redis.clients.jedis.CommandMetadataResolverFactory;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.commands.ProtocolCommands;
import redis.clients.jedis.util.SafeEncoder;

/**
 * A {@link Cacheable} that resolves the cacheability verdict of every command known to Jedis into a
 * materialized map. A command is cacheable only when all of the following hold: no
 * {@code dont_cache} tip, the {@code readonly} flag, no {@code blocking} flag, a key-name argument,
 * no {@code nondeterministic_output} tip, and no {@code script_runner} flag.
 * <p>
 * Verdicts come from the metadata supplied by the {@link CommandMetadataResolver} (which itself
 * applies the known server metadata fixes). Caller-supplied excludedCommands can only narrow the
 * eligible set: overridden commands are recorded as not cacheable regardless of their metadata
 * verdict. A command without metadata has no verdict: it fails closed and is logged once per
 * command name. Metadata for more commands is supplied through the resolver itself, see
 * {@link CommandMetadataResolver#withFallback(CommandMetadataResolver)}.
 */
class CacheabilityResolver implements Cacheable {

  private static final Logger logger = LoggerFactory.getLogger(CacheabilityResolver.class);

  // Shared default policy, built on first use rather than in a static initializer so a
  // metadata-load failure surfaces to the caller instead of poisoning this class
  private static volatile CacheabilityResolver DEFAULT_RESOLVER;

  /**
   * The shared default policy over the shared command metadata, built on first use.
   * @throws IllegalStateException when the command metadata failed to load, carrying the load
   *           failure as its cause; thrown on every call, the policy never runs on wrong metadata
   */
  static CacheabilityResolver defaultResolver() {
    CacheabilityResolver resolver = DEFAULT_RESOLVER;
    if (resolver == null) {
      synchronized (CacheabilityResolver.class) {
        resolver = DEFAULT_RESOLVER;
        if (resolver == null) {
          resolver = new CacheabilityResolver(CommandMetadataResolverFactory.defaultResolver());
          DEFAULT_RESOLVER = resolver;
        }
      }
    }
    return resolver;
  }

  private final CommandMetadataResolver metadataResolver;
  private final Set<ProtocolCommand> excludedCommands;
  private final Map<ProtocolCommand, Boolean> cacheabilityMap;
  // user intent: consulted only for commands the rules allow, so it can narrow but never widen
  private final Cacheable narrowing;

  /** Unknown command names already warned about, so the hot path logs each name only once. */
  private final Set<String> unknownWarned = ConcurrentHashMap.newKeySet();

  CacheabilityResolver(CommandMetadataResolver metadataResolver) {
    this(metadataResolver, null);
  }

  /**
   * @param excludedCommands per-command verdicts that win over any metadata-derived verdict;
   *          copied, later changes to the given map have no effect; may be null or empty for none
   */
  CacheabilityResolver(CommandMetadataResolver metadataResolver,
      Set<ProtocolCommand> excludedCommands) {
    this(metadataResolver, excludedCommands, null);
  }

  private CacheabilityResolver(CommandMetadataResolver metadataResolver,
      Set<ProtocolCommand> excludedCommands, Cacheable narrowing) {
    this.narrowing = narrowing;
    this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver");
    this.excludedCommands = excludedCommands == null || excludedCommands.isEmpty() ? null
        : new HashSet<>(excludedCommands);
    this.cacheabilityMap = Collections.unmodifiableMap(resolve());
  }

  CacheabilityResolver withExclusions(Set<ProtocolCommand> excludedCommands, Cacheable narrowing) {
    return (excludedCommands == null || excludedCommands.isEmpty()) && narrowing == null ? this
        : new CacheabilityResolver(metadataResolver, excludedCommands, narrowing);
  }

  // same exclusions over another metadata source; the shared default policy is reused as is
  CacheabilityResolver withMetadataResolver(CommandMetadataResolver metadataResolver) {
    return metadataResolver == this.metadataResolver ? this
        : new CacheabilityResolver(metadataResolver, excludedCommands, narrowing);
  }

  /**
   * Judges by the given command only; a declared subcommand is not visible through this signature.
   * Prefer {@link #isCacheable(CommandObject)}, which judges container subcommands by their own
   * metadata.
   */
  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    Boolean verdict = cacheabilityMap.get(command);
    if (verdict == null) {
      logUnknownCommandName(command);
      return false;
    }
    return verdict && (narrowing == null || narrowing.isCacheable(command, keys));
  }

  @Override
  public boolean isCacheable(CommandObject<?> commandObject) {
    ProtocolCommand command = effectiveCommand(commandObject);
    Boolean verdict = cacheabilityMap.get(command);
    if (verdict == null) {
      logUnknownCommandName(command);
      return false;
    }
    return verdict && (narrowing == null || narrowing.isCacheable(commandObject));
  }

  boolean isEligible(ProtocolCommand command) {
    return Boolean.TRUE.equals(cacheabilityMap.get(command));
  }

  ProtocolCommand effectiveCommand(CommandObject<?> commandObject) {
    return getWithSubcommand(commandObject.getArguments());
  }

  private ProtocolCommand getWithSubcommand(CommandArguments commandArguments) {
    if (commandArguments.getSubcommand() == null) {
      return commandArguments.getCommand();
    }

    return ProtocolSubcommand.getSubcommand(commandArguments.getCommand(),
      commandArguments.getSubcommand());
  }

  private void logUnknownCommandName(ProtocolCommand command) {
    String name = toString(command);
    if (unknownWarned.add(name)) {
      logger.warn("Command {} is unknown to the client-side caching policy; not caching it.", name);
    }
  }

  private static String toString(ProtocolCommand command) {
    return SafeEncoder.encode(command.getRaw());
  }

  /**
   * Final verdict for every command known to Jedis, built in layers: first every command is judged
   * by the metadata from the {@link CommandMetadataResolver} (known server metadata fixes already
   * applied), then the caller-supplied exclusions are recorded as not cacheable. Commands without
   * metadata are absent, so lookups on them fail closed.
   */
  Map<ProtocolCommand, Boolean> resolve() {
    Map<ProtocolCommand, Boolean> cacheabilityMap = new HashMap<>();
    Map<String, ProtocolCommand> canonicalCommands = new HashMap<>();
    for (ProtocolCommand[] values : ProtocolCommands.allCommands()) {
      for (ProtocolCommand command : values) {
        CommandMetadata metadata = metadataResolver.resolve(toString(command));
        if (metadata != null) {
          cacheabilityMap.put(command, isClientSideCacheable(metadata));
          canonicalCommands.put(metadata.getName(), command);
          metadata.getSubcommands().forEach(sub -> {
            ProtocolSubcommand subcommand = new ProtocolSubcommand(sub.getName());
            cacheabilityMap.put(subcommand, isClientSideCacheable(sub));
            canonicalCommands.put(sub.getName(), subcommand);
          });
        }
      }
    }
    applyExclusions(cacheabilityMap, canonicalCommands);
    return cacheabilityMap;
  }

  private void applyExclusions(Map<ProtocolCommand, Boolean> cacheabilityMap,
      Map<String, ProtocolCommand> canonicalCommands) {
    if (excludedCommands == null) {
      return;
    }
    for (ProtocolCommand protocolCommand : excludedCommands) {
      if (cacheabilityMap.containsKey(protocolCommand)) {
        cacheabilityMap.put(protocolCommand, false);
      } else {
        String command = toString(protocolCommand);
        ProtocolCommand canonical = canonicalCommands.get(command);
        if (canonical != null) {
          cacheabilityMap.put(canonical, false);
        }
      }
    }
  }

  /**
   * A pipe-merged {@code PARENT|CHILD} command. Value-equal by its raw bytes, so an instance built
   * from a metadata name and one built from a command plus declared subcommand produce
   * interchangeable map keys.
   */
  static final class ProtocolSubcommand implements ProtocolCommand {

    /**
     * Merged {@code PARENT|CHILD} instances memoized per (command, subcommand) pair, so the hot
     * path builds each merged command once instead of on every lookup. Bounded to Jedis' own
     * command enums; foreign {@link ProtocolCommand} implementations are merged per call to keep
     * the cache from growing without limit.
     */
    private static final Map<ProtocolCommand, Map<Keyword, ProtocolSubcommand>> mergedCommands = new ConcurrentHashMap<>();

    private final byte[] raw;
    private final int hashCode;

    ProtocolSubcommand(String name) {
      this(SafeEncoder.encode(name));
    }

    public static ProtocolCommand getSubcommand(ProtocolCommand command, Keyword subcommand) {
      // this is guarding against the case where a foreign ProtocolCommand implementation is passed
      // in, which we don't want to cache forever
      if (!(command instanceof Enum)) {
        return new ProtocolSubcommand(command, subcommand);
      }
      return mergedCommands.computeIfAbsent(command, c -> new ConcurrentHashMap<>())
          .computeIfAbsent(subcommand, s -> new ProtocolSubcommand(command, s));
    }

    ProtocolSubcommand(ProtocolCommand command, Keyword subcommand) {
      this(mergeWithPipe(command.getRaw(), subcommand.getRaw()));
    }

    private ProtocolSubcommand(byte[] raw) {
      this.raw = raw;
      this.hashCode = Arrays.hashCode(raw);
    }

    private static byte[] mergeWithPipe(byte[] cmdBytes, byte[] subBytes) {
      byte[] merged = new byte[cmdBytes.length + 1 + subBytes.length];
      System.arraycopy(cmdBytes, 0, merged, 0, cmdBytes.length);
      merged[cmdBytes.length] = '|';
      System.arraycopy(subBytes, 0, merged, cmdBytes.length + 1, subBytes.length);
      return merged;
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    /**
     * A command is identified by its raw wire bytes, so this compares equal to any
     * {@link ProtocolCommand} with the same raw — regardless of the implementing class — the way
     * {@code java.util.List} defines equality across implementations. Implementations with
     * identity-based equality (enums, lambdas) cannot reciprocate; lookups probing with such
     * instances resolve by their own semantics.
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ProtocolCommand)) {
        return false;
      }
      return Arrays.equals(raw, ((ProtocolCommand) obj).getRaw());
    }
  }

  static boolean isClientSideCacheable(CommandMetadata m) {
    return !m.hasTip("dont_cache") //
        && m.hasFlag("readonly") //
        && !m.hasFlag("blocking") //
        && hasKeyArgument(m) //
        && !m.hasTip("nondeterministic_output") //
        && !m.hasFlag("script_runner");
  }

  private static boolean hasKeyArgument(CommandMetadata m) {
    return m.hasKeyNameSpec() || (m.getFirstKey() > 0 && m.getStep() > 0);
  }
}
