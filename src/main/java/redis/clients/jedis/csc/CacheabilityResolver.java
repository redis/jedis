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
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.MetadataResolver;
import redis.clients.jedis.MetadataResolver.CommandMetadata;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.bloom.RedisBloomProtocol.BloomFilterCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.CountMinSketchCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.CuckooFilterCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.TDigestCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.TopKCommand;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.json.JsonProtocol.JsonCommand;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.SafeEncoder;

/**
 * A {@link Cacheable} that resolves the cacheability verdict of every command known to Jedis into a
 * materialized map. A command is cacheable only when all of the following hold: no
 * {@code dont_cache} tip, the {@code readonly} flag, no {@code blocking} flag, a key-name argument,
 * no {@code nondeterministic_output} tip, and no {@code script_runner} flag.
 * <p>
 * Verdicts come from the metadata supplied by the {@link MetadataResolver} (which itself applies
 * the known server metadata fixes). Caller-supplied excludedCommands can only narrow the eligible
 * set: overridden commands are recorded as not cacheable regardless of their metadata verdict. When
 * a command has no verdict, the fallback {@link Cacheable} decides; without a fallback such
 * commands fail closed and are logged once per command name.
 */
class CacheabilityResolver implements Cacheable {

  private static final Logger logger = LoggerFactory.getLogger(CacheabilityResolver.class);

  /**
   * Every {@link ProtocolCommand} enum shipped by Jedis; extend when a new protocol enum is added.
   */
  private static final List<ProtocolCommand[]> PROTOCOL_ENUMS = Arrays.asList(Command.values(),
    JsonCommand.values(), SearchCommand.values(), TimeSeriesCommand.values(),
    BloomFilterCommand.values(), CuckooFilterCommand.values(), CountMinSketchCommand.values(),
    TopKCommand.values(), TDigestCommand.values());

  public static Cacheable DEFAULT_RESOLVER = new CacheabilityResolver(new MetadataResolver());

  /**
   * A pipe-merged {@code PARENT|CHILD} command. Value-equal by its raw bytes, so any two
   * {@link CommandArguments} built from the same command and subcommand produce interchangeable map
   * keys.
   */
  static final class ProtocolSubcommand implements ProtocolCommand {

    private final byte[] raw;
    private final int hashCode;

    ProtocolSubcommand(ProtocolCommand command, Keyword subcommand) {
      byte[] cmdBytes = command.getRaw();
      byte[] subBytes = subcommand.getRaw();

      byte[] merged = new byte[cmdBytes.length + 1 + subBytes.length];
      System.arraycopy(cmdBytes, 0, merged, 0, cmdBytes.length);
      merged[cmdBytes.length] = '|';
      System.arraycopy(subBytes, 0, merged, cmdBytes.length + 1, subBytes.length);

      this.raw = merged;
      this.hashCode = Arrays.hashCode(raw);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

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

  private final MetadataResolver metadataResolver;
  private final Set<ProtocolCommand> excludedCommands;
  private final Map<ProtocolCommand, Boolean> cacheabilityMap;
  private final Cacheable fallback;

  /** Unknown command names already warned about, so the hot path logs each name only once. */
  private final Set<String> unknownWarned = ConcurrentHashMap.newKeySet();

  CacheabilityResolver(MetadataResolver metadataResolver) {
    this(metadataResolver, null, null);
  }

  /**
   * @param excludedCommands per-command verdicts that win over any metadata-derived verdict;
   *          copied, later changes to the given map have no effect; may be null or empty for none
   */
  CacheabilityResolver(MetadataResolver metadataResolver, Set<ProtocolCommand> excludedCommands,
      Cacheable fallback) {
    this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver");
    this.excludedCommands = excludedCommands == null || excludedCommands.isEmpty() ? null
        : new HashSet<>(excludedCommands);
    this.cacheabilityMap = Collections.unmodifiableMap(resolve());
    this.fallback = fallback;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    throw new UnsupportedOperationException("Use isCacheable(CommandObject) instead");
  }

  @Override
  public boolean isCacheable(CommandObject<?> commandObject) {
    ProtocolCommand protocolCommand = getWithSubcommand(commandObject.getArguments());
    Boolean verdict = cacheabilityMap.get(protocolCommand);
    if (verdict == null && fallback != null) {
      verdict = fallback.isCacheable(commandObject);
    }
    if (verdict == null) {
      logUnknownCommandName(protocolCommand);
      return false;
    }
    return verdict;
  }

  private ProtocolCommand getWithSubcommand(CommandArguments commandArguments) {
    if (commandArguments.getSubcommand() == null) {
      return commandArguments.getCommand();
    }
    return new ProtocolSubcommand(commandArguments.getCommand(), commandArguments.getSubcommand());
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
   * by the metadata from the {@link MetadataResolver} (known server metadata fixes already
   * applied), then the caller-supplied exclusions are recorded as not cacheable. Commands without
   * metadata are absent, so lookups on them fall through to the fallback or fail closed.
   */
  Map<ProtocolCommand, Boolean> resolve() {
    Map<ProtocolCommand, Boolean> cacheabilityMap = new HashMap<>();
    for (ProtocolCommand[] values : PROTOCOL_ENUMS) {
      for (ProtocolCommand command : values) {
        CommandMetadata metadata = metadataResolver.resolve(toString(command));
        if (metadata != null) {
          cacheabilityMap.put(command, isClientSideCacheable(metadata));
          metadata.getSubcommands().forEach(sub -> cacheabilityMap
              .put(new ProtocolSubCommand(sub.getName()), isClientSideCacheable(sub)));

        }
      }
    }
    applyExclusions(cacheabilityMap);
    return cacheabilityMap;
  }

  private void applyExclusions(Map<ProtocolCommand, Boolean> cacheabilityMap) {
    if (excludedCommands != null) {
      for (ProtocolCommand protocolCommand : excludedCommands) {
        cacheabilityMap.put(protocolCommand, false);
        CommandMetadata metadata = metadataResolver.resolve(toString(protocolCommand));
        if (metadata != null) {
          metadata.getSubcommands()
              .forEach(sub -> cacheabilityMap.put(new ProtocolSubCommand(sub.getName()), false));
        }
      }
    }
  }

  private static class ProtocolSubCommand implements ProtocolCommand {
    private final byte[] raw;

    ProtocolSubCommand(String name) {
      this.raw = SafeEncoder.encode(name);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(raw);
    }

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
