package redis.clients.jedis;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.bloom.RedisBloomProtocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.json.JsonProtocol;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.timeseries.TimeSeriesProtocol;

/**
 * Shared command-metadata resolver: a lookup table of normalized Redis {@code COMMAND} metadata
 * keyed by command name, generated from a known Redis release by {@code CommandMetadataUtil} (test
 * sources). Container subcommands are keyed {@code PARENT|CHILD} (for example
 * {@code MEMORY|USAGE}).
 * <p>
 * The resolver preserves the server-provided metadata as-is and applies no feature policy itself;
 * consumers such as cache eligibility resolvers and feature policy evaluators evaluate the resolved
 * metadata. An optional override table takes precedence over the generated metadata, for commands
 * whose server metadata is known to be wrong or incomplete.
 */
@Internal
public final class MetadataResolver {

  /** Normalized metadata of a single command. */
  public static final class CommandMetadata {

    private final String name;
    private Set<String> flags;
    private Set<String> tips;
    private final long firstKey;
    private final long lastKey;
    private final long step;
    private final boolean hasKeyNameSpec;

    /** Subcommands of a container command, kept in hierarchy as the server reports them. */
    private final Set<CommandMetadata> subcommands = new HashSet<>();

    /**
     * @param hasKeyNameSpec whether at least one key spec names a key (i.e. is not flagged
     *          {@code not_key}); full key specs are recorded in {@code CommandMetadata.json}
     */
    CommandMetadata(String name, Set<String> flags, Set<String> tips, long firstKey, long lastKey,
        long step, boolean hasKeyNameSpec) {
      this.name = name;
      this.flags = flags;
      this.tips = tips;
      this.firstKey = firstKey;
      this.lastKey = lastKey;
      this.step = step;
      this.hasKeyNameSpec = hasKeyNameSpec;
    }

    /**
     * Adds tips to the existing set, for commands whose server metadata is known to be incomplete.
     * Applied as a later resolution step, so the server-provided flags, tips, and key metadata are
     * otherwise respected.
     */
    void additionalTips(String[] additionalTips) {
      Set<String> merged = new HashSet<>(tips);
      for (String tip : additionalTips) {
        merged.add(tip);
      }
      tips = merged;
    }

    void seal() {
      flags = Collections.unmodifiableSet(flags);
      tips = Collections.unmodifiableSet(tips);
    }

    void addSubcommand(CommandMetadata subcommand) {
      subcommands.add(subcommand);
    }

    public Set<CommandMetadata> getSubcommands() {
      return subcommands;
    }

    public String getName() {
      return name;
    }

    public Set<String> getFlags() {
      return flags;
    }

    public Set<String> getTips() {
      return tips;
    }

    public boolean hasFlag(String flag) {
      return flags.contains(flag);
    }

    public boolean hasTip(String tip) {
      return tips.contains(tip);
    }

    public long getFirstKey() {
      return firstKey;
    }

    public long getLastKey() {
      return lastKey;
    }

    public long getStep() {
      return step;
    }

    public boolean hasKeyNameSpec() {
      return hasKeyNameSpec;
    }
  }

  /**
   * Environment variable naming a JSON file with the same layout as {@code CommandMetadata.json}
   * (produced by {@code CommandMetadataUtil}). When set, the command table is read from that file
   * instead of the generated table; the file is read and parsed once, on first use.
   */
  static final String COMMAND_METADATA_PATH_ENV = "JEDIS_COMMAND_METADATA_PATH";

  /** Lazy singleton holder: the table is built on first use and shared by all instances. */
  private static final class CommandTable {

    private static final Map<String, CommandMetadata> INSTANCE = init();
    private static Exception INIT_FAILURE;

    private static Map<String, CommandMetadata> load() {
      String path = System.getenv(COMMAND_METADATA_PATH_ENV);
      if (path == null || path.trim().isEmpty()) {
        return generatedCommandTable();
      }
      return MetadataReader.read(Paths.get(path.trim()));
    }

    private static Map<String, CommandMetadata> applyKnownMetadataFixes(
        Map<String, CommandMetadata> table) {
      for (Map.Entry<String, String[]> entry : KNOWN_SERVER_METADATA_FIXES.entrySet()) {
        String name = entry.getKey();
        CommandMetadata metadata = table.get(name);
        if (metadata != null) {
          metadata.additionalTips(entry.getValue());
        }
      }
      return table;
    }

    private static Map<String, CommandMetadata> seal(Map<String, CommandMetadata> map) {
      map.values().forEach(md -> md.seal());

      return Collections.unmodifiableMap(map);
    }

    private static Map<String, CommandMetadata> init() {
      try {
        Map<String, CommandMetadata> map = load();
        map = applyKnownMetadataFixes(map);
        return seal(map);
      } catch (Exception e) {
        INIT_FAILURE = e;
        LoggerFactory.getLogger(MetadataResolver.class).error(
          "Failed to load command metadata properly; command metadata is unavailable and all consumers relying on it will fail.",
          e);
      }
      return null;
    }
  }

  /**
   * Tips added on top of the resolved metadata for commands whose server metadata is known to be
   * incomplete. Applied as a later resolution step, so the server-provided flags, tips, and key
   * metadata are otherwise respected. Keep in sync with {@code docs/csc-command-cacheability.md}
   * and {@code CommandMetadataUtil}; remove an entry once the corresponding server metadata is
   * fixed.
   */
  private static final Map<String, String[]> KNOWN_SERVER_METADATA_FIXES = new HashMap<>();
  static {
    // by design: mutates key idle time; reply is an existence count aggregated across shards
    KNOWN_SERVER_METADATA_FIXES.put("TOUCH", new String[] { "dont_cache" });
    // random reply, but server metadata lacks the nondeterministic_output tip
    KNOWN_SERVER_METADATA_FIXES.put("VRANDMEMBER", new String[] { "nondeterministic_output" });
    // BY/GET pattern keys are external to the declared keys ("unknown" key spec), so cached
    // replies can never be invalidated when those keys change; deny until tracking covers them
    KNOWN_SERVER_METADATA_FIXES.put("SORT_RO", new String[] { "dont_cache" });
  }

  /**
   * Every {@link ProtocolCommand} enum shipped by Jedis; extend when a new protocol enum is added.
   * Single source of truth for all consumers (cacheability resolution, the metadata tool's mismatch
   * report).
   */
  private static final List<ProtocolCommand[]> PROTOCOL_ENUMS = Arrays.asList(
    Protocol.Command.values(), JsonProtocol.JsonCommand.values(),
    SearchProtocol.SearchCommand.values(), TimeSeriesProtocol.TimeSeriesCommand.values(),
    RedisBloomProtocol.BloomFilterCommand.values(), RedisBloomProtocol.CuckooFilterCommand.values(),
    RedisBloomProtocol.CountMinSketchCommand.values(), RedisBloomProtocol.TopKCommand.values(),
    RedisBloomProtocol.TDigestCommand.values());

  /** All {@link ProtocolCommand} enums shipped by Jedis. */
  @Internal
  public static List<ProtocolCommand[]> protocolCommandEnums() {
    return PROTOCOL_ENUMS;
  }

  /**
   * The effective command table, for same-package consumers such as the flags registrar.
   * @throws IllegalStateException when the command metadata failed to load (see the error logged at
   *           first use); by design consumers must fail rather than run with wrong information
   */
  static Map<String, CommandMetadata> commandTable() {
    if (CommandTable.INSTANCE == null) {
      throw new IllegalStateException("Command metadata is unavailable because loading it failed"
          + " (typically a bad " + COMMAND_METADATA_PATH_ENV + " file; see the error logged at"
          + " first use). Client-side caching and the command flags registry cannot operate"
          + " without it.", CommandTable.INIT_FAILURE);
    }
    return CommandTable.INSTANCE;
  }

  /**
   * The known server metadata fixes (command name to the tips the fix adds), for same-package
   * consumers such as the metadata tool's obsolescence check.
   */
  static Map<String, String[]> knownServerMetadataFixes() {
    return Collections.unmodifiableMap(KNOWN_SERVER_METADATA_FIXES);
  }

  /** Freshly built generated table, independent of the configured metadata source. */
  static Map<String, CommandMetadata> generatedCommandTable() {
    Map<String, CommandMetadata> table = new HashMap<>(1024);
    registerAll(table);
    return table;
  }

  public MetadataResolver() {
  }

  /**
   * @return metadata for the command, or {@code null} when the command is unknown
   * @throws IllegalStateException when the command metadata failed to load (see the error logged at
   *           first use); by design consumers must fail rather than run with wrong information
   */
  public CommandMetadata resolve(String commandName) {
    return commandTable().get(commandName);
  }

  private static CommandMetadata add(Map<String, CommandMetadata> map, String name,
      Set<String> flags, Set<String> tips, long firstKey, long lastKey, long step,
      boolean hasKeyNameSpec) {
    CommandMetadata metadata = new CommandMetadata(name, flags, tips, firstKey, lastKey, step,
        hasKeyNameSpec);
    map.put(name, metadata);
    return metadata;
  }

  // GENERATED-METADATA-BEGIN (managed by CommandMetadataUtil, do not edit)
  // Source: CommandMetadata.json (Redis 8.10.0). Generated at 2026-08-20 13:52:45 TRT.
  private static void init0(Map<String, CommandMetadata> t) {
    CommandMetadata aclMetadata = add(t, "ACL", Collections.emptySet(), Collections.emptySet(), 0,
      0, 0, false);
    aclMetadata.addSubcommand(
      add(t, "ACL|CAT", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|DELUSER", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|DRYRUN", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|GENPASS", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|GETUSER", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(add(t, "ACL|HELP", new HashSet<>(Arrays.asList("loading", "stale")),
      Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|LIST", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|LOAD", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|LOG", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|SAVE", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|SETUSER", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|USERS", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    aclMetadata.addSubcommand(
      add(t, "ACL|WHOAMI", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    add(t, "APPEND", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ARCOUNT", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "ARDEL", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARDELRANGE", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARGET", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ARGETRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARGREP", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARINFO", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARINSERT", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ARLASTITEMS", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARLEN", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ARMGET", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ARMSET", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ARNEXT", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "AROP", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1, true);
    add(t, "ARRING", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ARSCAN", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARSEEK", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ARSET", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ASKING", new HashSet<>(Arrays.asList("fast")), Collections.emptySet(), 0, 0, 0, false);
    add(t, "AUTH",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "no_auth", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    CommandMetadata backupMetadata = add(t, "BACKUP", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    backupMetadata.addSubcommand(add(t, "BACKUP|ABORT",
      new HashSet<>(Arrays.asList("admin", "noscript")), Collections.emptySet(), 0, 0, 0, false));
    backupMetadata.addSubcommand(add(t, "BACKUP|CLEANUP",
      new HashSet<>(Arrays.asList("admin", "noscript")), Collections.emptySet(), 0, 0, 0, false));
    backupMetadata.addSubcommand(add(t, "BACKUP|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    backupMetadata.addSubcommand(add(t, "BACKUP|LIST",
      new HashSet<>(Arrays.asList("admin", "stale")), Collections.emptySet(), 0, 0, 0, false));
    backupMetadata.addSubcommand(add(t, "BACKUP|SEAL",
      new HashSet<>(Arrays.asList("admin", "noscript")), Collections.emptySet(), 0, 0, 0, false));
    backupMetadata.addSubcommand(add(t, "BACKUP|START",
      new HashSet<>(Arrays.asList("admin", "noscript")), Collections.emptySet(), 0, 0, 0, false));
    backupMetadata.addSubcommand(add(t, "BACKUP|STATUS",
      new HashSet<>(Arrays.asList("admin", "stale")), Collections.emptySet(), 0, 0, 0, false));
    add(t, "BF.ADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.CARD", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.DEBUG", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "BF.EXISTS", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.INFO", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "BF.INSERT", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.LOADCHUNK", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.MADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.MEXISTS", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.RESERVE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "BF.SCANDUMP", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "BGREWRITEAOF", new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "BGSAVE", new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "BITCOUNT", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "BITFIELD", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "BITFIELD_RO", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "BITOP", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 2, -1,
      1, true);
    add(t, "BITPOS", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "BLMOVE", new HashSet<>(Arrays.asList("blocking", "denyoom", "write")),
      Collections.emptySet(), 1, 2, 1, true);
    add(t, "BLMOVEM", new HashSet<>(Arrays.asList("blocking", "denyoom", "write")),
      Collections.emptySet(), 1, 2, 1, true);
    add(t, "BLMPOP", new HashSet<>(Arrays.asList("blocking", "movablekeys", "write")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "BLPOP", new HashSet<>(Arrays.asList("blocking", "write")), Collections.emptySet(), 1,
      -2, 1, true);
    add(t, "BRPOP", new HashSet<>(Arrays.asList("blocking", "write")), Collections.emptySet(), 1,
      -2, 1, true);
    add(t, "BRPOPLPUSH", new HashSet<>(Arrays.asList("blocking", "denyoom", "write")),
      Collections.emptySet(), 1, 2, 1, true);
    add(t, "BZMPOP", new HashSet<>(Arrays.asList("blocking", "movablekeys", "write")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "BZPOPMAX", new HashSet<>(Arrays.asList("blocking", "fast", "write")),
      Collections.emptySet(), 1, -2, 1, true);
    add(t, "BZPOPMIN", new HashSet<>(Arrays.asList("blocking", "fast", "write")),
      Collections.emptySet(), 1, -2, 1, true);
    add(t, "CF.ADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.ADDNX", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.COMPACT", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.COUNT", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.DEBUG", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "CF.DEL", new HashSet<>(Arrays.asList("fast", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.EXISTS", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.INFO", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "CF.INSERT", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.INSERTNX", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.LOADCHUNK", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.MEXISTS", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.RESERVE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CF.SCANDUMP", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    CommandMetadata clientMetadata = add(t, "CLIENT", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    clientMetadata.addSubcommand(
      add(t, "CLIENT|CACHING", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|GETNAME", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|GETREDIR", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(add(t, "CLIENT|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|ID", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|INFO", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|KILL", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|LIST", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clientMetadata.addSubcommand(add(t, "CLIENT|NO-EVICT",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")), Collections.emptySet(),
      0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|NO-TOUCH", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|PAUSE", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|REPLY", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|SETINFO", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|SETNAME", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|TRACKING", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(
      add(t, "CLIENT|TRACKINGINFO", new HashSet<>(Arrays.asList("loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clientMetadata.addSubcommand(add(t, "CLIENT|UNBLOCK",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")), Collections.emptySet(),
      0, 0, 0, false));
    clientMetadata.addSubcommand(add(t, "CLIENT|UNPAUSE",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")), Collections.emptySet(),
      0, 0, 0, false));
    CommandMetadata clusterMetadata = add(t, "CLUSTER", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|ADDSLOTS", new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|ADDSLOTSRANGE",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|BUMPEPOCH",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|COUNT-FAILURE-REPORTS",
      new HashSet<>(Arrays.asList("admin", "loading", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|COUNTKEYSINSLOT",
      new HashSet<>(Arrays.asList("stale")), Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|DELSLOTS", new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|DELSLOTSRANGE",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|FAILOVER", new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|FLUSHSLOTS",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|FORGET", new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|GETKEYSINSLOT", new HashSet<>(Arrays.asList("stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|INFO", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|KEYSLOT",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|LINKS", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|MEET", new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|MIGRATION",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|MYID",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|MYSHARDID", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|NODES", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|REPLICAS", new HashSet<>(Arrays.asList("admin", "loading", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|REPLICATE",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|RESET", new HashSet<>(Arrays.asList("admin", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|SAVECONFIG",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|SET-CONFIG-EPOCH",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")), Collections.emptySet(), 0,
      0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|SETSLOT", new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|SHARDS", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|SLAVES", new HashSet<>(Arrays.asList("admin", "loading", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(
      add(t, "CLUSTER|SLOT-STATS", new HashSet<>(Arrays.asList("loading", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_shards")), 0, 0,
        0, false));
    clusterMetadata
        .addSubcommand(add(t, "CLUSTER|SLOTS", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    clusterMetadata.addSubcommand(add(t, "CLUSTER|SYNCSLOTS",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false));
    add(t, "CMS.INCRBY", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CMS.INFO", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "CMS.INITBYDIM", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CMS.INITBYPROB", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CMS.MERGE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "CMS.QUERY", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    CommandMetadata commandMetadata = add(t, "COMMAND",
      new HashSet<>(Arrays.asList("loading", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 0, 0, 0, false);
    commandMetadata.addSubcommand(add(t, "COMMAND|COUNT",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    commandMetadata
        .addSubcommand(add(t, "COMMAND|DOCS", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output_order")), 0, 0, 0, false));
    commandMetadata.addSubcommand(add(t, "COMMAND|GETKEYS",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    commandMetadata.addSubcommand(add(t, "COMMAND|GETKEYSANDFLAGS",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    commandMetadata.addSubcommand(add(t, "COMMAND|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    commandMetadata
        .addSubcommand(add(t, "COMMAND|INFO", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output_order")), 0, 0, 0, false));
    commandMetadata
        .addSubcommand(add(t, "COMMAND|LIST", new HashSet<>(Arrays.asList("loading", "stale")),
          new HashSet<>(Arrays.asList("nondeterministic_output_order")), 0, 0, 0, false));
    CommandMetadata configMetadata = add(t, "CONFIG", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    configMetadata.addSubcommand(
      add(t, "CONFIG|GET", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    configMetadata.addSubcommand(add(t, "CONFIG|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    configMetadata.addSubcommand(add(t, "CONFIG|RESETSTAT",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")), 0,
      0, 0, false));
    configMetadata.addSubcommand(add(t, "CONFIG|REWRITE",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")), 0,
      0, 0, false));
    configMetadata.addSubcommand(
      add(t, "CONFIG|SET", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    add(t, "COPY", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 2,
      1, true);
    add(t, "DBSIZE", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:agg_sum")), 0, 0, 0,
      false);
    add(t, "DEBUG", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "DECR", new HashSet<>(Arrays.asList("denyoom", "fast", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "DECRBY", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "DEL", new HashSet<>(Arrays.asList("write")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard", "response_policy:agg_sum")), 1, -1,
      1, true);
    add(t, "DELEX", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "DIGEST", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "DISCARD",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "DUMP", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "ECHO", new HashSet<>(Arrays.asList("fast", "loading", "stale")), Collections.emptySet(),
      0, 0, 0, false);
    add(t, "EVAL", new HashSet<>(Arrays.asList("movablekeys", "no_mandatory_keys", "noscript",
      "script_runner", "skip_monitor", "stale")), Collections.emptySet(), 0, 0, 0, true);
    add(t, "EVALSHA", new HashSet<>(Arrays.asList("movablekeys", "no_mandatory_keys", "noscript",
      "script_runner", "skip_monitor", "stale")), Collections.emptySet(), 0, 0, 0, true);
    add(
      t, "EVALSHA_RO", new HashSet<>(Arrays.asList("movablekeys", "no_mandatory_keys", "noscript",
        "readonly", "script_runner", "skip_monitor", "stale")),
      Collections.emptySet(), 0, 0, 0, true);
    add(
      t, "EVAL_RO", new HashSet<>(Arrays.asList("movablekeys", "no_mandatory_keys", "noscript",
        "readonly", "script_runner", "skip_monitor", "stale")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "EXEC", new HashSet<>(Arrays.asList("loading", "noscript", "skip_slowlog", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "EXISTS", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard", "response_policy:agg_sum")), 1, -1,
      1, true);
    add(t, "EXPIRE", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "EXPIREAT", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "EXPIRETIME", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "FAILOVER", new HashSet<>(Arrays.asList("admin", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "FCALL", new HashSet<>(Arrays.asList("movablekeys", "no_mandatory_keys", "noscript",
      "script_runner", "skip_monitor", "stale")), Collections.emptySet(), 0, 0, 0, true);
    add(
      t, "FCALL_RO", new HashSet<>(Arrays.asList("movablekeys", "no_mandatory_keys", "noscript",
        "readonly", "script_runner", "skip_monitor", "stale")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "FLUSHALL", new HashSet<>(Arrays.asList("write")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")), 0,
      0, 0, false);
    add(t, "FLUSHDB", new HashSet<>(Arrays.asList("write")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")), 0,
      0, 0, false);
    add(t, "FT.ADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 2, 2, 1, true);
    add(t, "FT.AGGREGATE", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.ALIASADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.ALIASDEL", new HashSet<>(Arrays.asList("module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.ALIASLIST", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.ALIASUPDATE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.ALTER", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    CommandMetadata ftConfigMetadata = add(t, "FT.CONFIG",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false);
    ftConfigMetadata
        .addSubcommand(add(t, "FT.CONFIG|GET", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    ftConfigMetadata
        .addSubcommand(add(t, "FT.CONFIG|HELP", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    ftConfigMetadata
        .addSubcommand(add(t, "FT.CONFIG|SET", new HashSet<>(Arrays.asList("module", "write")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    add(t, "FT.CREATE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    CommandMetadata ftCursorMetadata = add(t, "FT.CURSOR",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false);
    ftCursorMetadata
        .addSubcommand(add(t, "FT.CURSOR|DEL", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache", "request_policy:special")), 0, 0, 0, false));
    ftCursorMetadata
        .addSubcommand(add(t, "FT.CURSOR|GC", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    ftCursorMetadata
        .addSubcommand(add(t, "FT.CURSOR|READ", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache", "request_policy:special")), 0, 0, 0, false));
    add(t, "FT.DEL", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(), 2, 2,
      1, true);
    add(t, "FT.DICTADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.DICTDEL", new HashSet<>(Arrays.asList("module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.DICTDUMP", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.DROP", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(), 0, 0,
      0, false);
    add(t, "FT.DROPINDEX", new HashSet<>(Arrays.asList("module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.EXPLAIN", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.EXPLAINCLI", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.GET", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 2, 2, 1, true);
    add(t, "FT.HYBRID", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.INFO", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.MGET", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.PROFILE", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.SAFEADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 2, 2, 1, true);
    add(t, "FT.SEARCH", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.SPELLCHECK", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.SUGADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "FT.SUGDEL", new HashSet<>(Arrays.asList("module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "FT.SUGGET", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "FT.SUGLEN", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "FT.SYNADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "FT.SYNDUMP", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.SYNUPDATE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT.TAGVALS", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT._ALIASADDIFNX", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT._ALIASDELIFX", new HashSet<>(Arrays.asList("module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT._ALTERIFNX", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT._CREATEIFNX", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT._DROPIFX", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      0, 0, 0, false);
    add(t, "FT._DROPINDEXIFX", new HashSet<>(Arrays.asList("module", "write")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "FT._LIST", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    CommandMetadata functionMetadata = add(t, "FUNCTION", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    functionMetadata.addSubcommand(
      add(t, "FUNCTION|DELETE", new HashSet<>(Arrays.asList("noscript", "write")),
        new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")),
        0, 0, 0, false));
    functionMetadata.addSubcommand(add(t, "FUNCTION|DUMP", new HashSet<>(Arrays.asList("noscript")),
      Collections.emptySet(), 0, 0, 0, false));
    functionMetadata.addSubcommand(
      add(t, "FUNCTION|FLUSH", new HashSet<>(Arrays.asList("noscript", "write")),
        new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")),
        0, 0, 0, false));
    functionMetadata.addSubcommand(add(t, "FUNCTION|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    functionMetadata.addSubcommand(
      add(t, "FUNCTION|KILL", new HashSet<>(Arrays.asList("allow_busy", "noscript")),
        new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:one_succeeded")),
        0, 0, 0, false));
    functionMetadata.addSubcommand(add(t, "FUNCTION|LIST", new HashSet<>(Arrays.asList("noscript")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 0, 0, 0, false));
    functionMetadata.addSubcommand(
      add(t, "FUNCTION|LOAD", new HashSet<>(Arrays.asList("denyoom", "noscript", "write")),
        new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")),
        0, 0, 0, false));
    functionMetadata.addSubcommand(
      add(t, "FUNCTION|RESTORE", new HashSet<>(Arrays.asList("denyoom", "noscript", "write")),
        new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")),
        0, 0, 0, false));
    functionMetadata.addSubcommand(
      add(t, "FUNCTION|STATS", new HashSet<>(Arrays.asList("allow_busy", "noscript")),
        new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_shards",
          "response_policy:special")),
        0, 0, 0, false));
    add(t, "GEOADD", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "GEODIST", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GEOHASH", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GEOPOS", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GEORADIUS", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "GEORADIUSBYMEMBER", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "GEORADIUSBYMEMBER_RO", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "GEORADIUS_RO", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "GEOSEARCH", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GEOSEARCHSTORE", new HashSet<>(Arrays.asList("denyoom", "write")),
      Collections.emptySet(), 1, 2, 1, true);
    add(t, "GET", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GETBIT", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "GETDEL", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
  }

  private static void init1(Map<String, CommandMetadata> t) {
    add(t, "GETEX", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GETRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "GETSET", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "HDEL", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "HELLO",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "no_auth", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "HEXISTS", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "HEXPIRE", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HEXPIREAT", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HEXPIRETIME", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "HGET", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HGETALL", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, 1, 1, true);
    add(t, "HGETDEL", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HGETEX", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    CommandMetadata himportMetadata = add(t, "HIMPORT", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    himportMetadata.addSubcommand(add(t, "HIMPORT|DISCARD", Collections.emptySet(),
      new HashSet<>(Arrays.asList("request_policy:all_shards")), 0, 0, 0, false));
    himportMetadata.addSubcommand(add(t, "HIMPORT|DISCARDALL", Collections.emptySet(),
      new HashSet<>(Arrays.asList("request_policy:all_shards")), 0, 0, 0, false));
    himportMetadata.addSubcommand(add(t, "HIMPORT|PREPARE", new HashSet<>(Arrays.asList("denyoom")),
      new HashSet<>(Arrays.asList("request_policy:all_shards")), 0, 0, 0, false));
    himportMetadata.addSubcommand(add(t, "HIMPORT|SET",
      new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 2, 2, 1, true));
    add(t, "HINCRBY", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "HINCRBYFLOAT", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "HKEYS", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, 1, 1, true);
    add(t, "HLEN", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HMGET", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HMSET", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    CommandMetadata hotkeysMetadata = add(t, "HOTKEYS", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    hotkeysMetadata.addSubcommand(add(t, "HOTKEYS|GET",
      new HashSet<>(Arrays.asList("admin", "noscript")), new HashSet<>(Arrays
          .asList("nondeterministic_output", "request_policy:special", "response_policy:special")),
      0, 0, 0, false));
    hotkeysMetadata.addSubcommand(add(t, "HOTKEYS|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    hotkeysMetadata
        .addSubcommand(add(t, "HOTKEYS|RESET", new HashSet<>(Arrays.asList("admin", "noscript")),
          new HashSet<>(Arrays.asList("request_policy:special")), 0, 0, 0, false));
    hotkeysMetadata
        .addSubcommand(add(t, "HOTKEYS|START", new HashSet<>(Arrays.asList("admin", "noscript")),
          new HashSet<>(Arrays.asList("request_policy:special")), 0, 0, 0, false));
    hotkeysMetadata
        .addSubcommand(add(t, "HOTKEYS|STOP", new HashSet<>(Arrays.asList("admin", "noscript")),
          new HashSet<>(Arrays.asList("request_policy:special")), 0, 0, 0, false));
    add(t, "HPERSIST", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HPEXPIRE", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "HPEXPIREAT", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "HPEXPIRETIME", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "HPTTL", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "HRANDFIELD", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "HSCAN", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "HSET", new HashSet<>(Arrays.asList("denyoom", "fast", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "HSETEX", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "HSETNX", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "HSTRLEN", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "HTTL", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "HVALS", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, 1, 1, true);
    add(t, "INCR", new HashSet<>(Arrays.asList("denyoom", "fast", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "INCRBY", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "INCRBYFLOAT", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "INCREX", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "INFO", new HashSet<>(Arrays.asList("loading", "stale")), new HashSet<>(Arrays
        .asList("nondeterministic_output", "request_policy:all_shards", "response_policy:special")),
      0, 0, 0, false);
    add(t, "JSON.ARRAPPEND", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.ARRINDEX", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.ARRINSERT", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.ARRLEN", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.ARRPOP", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.ARRTRIM", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.CLEAR", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "JSON.DEBUG", new HashSet<>(Arrays.asList("module", "movablekeys", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, true);
    add(t, "JSON.DEL", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "JSON.FORGET", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.GET", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.MERGE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.MGET", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.MSET", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, -1, 3, true);
    add(t, "JSON.NUMINCRBY", new HashSet<>(Arrays.asList("module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.NUMMULTBY", new HashSet<>(Arrays.asList("module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.NUMPOWBY", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.OBJKEYS", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.OBJLEN", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.RESP", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.SET", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.STRAPPEND", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.STRLEN", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "JSON.TOGGLE", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "JSON.TYPE", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "KEYS", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order", "request_policy:all_shards")), 0,
      0, 0, false);
    add(t, "LASTSAVE", new HashSet<>(Arrays.asList("fast", "loading", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false);
    CommandMetadata latencyMetadata = add(t, "LATENCY", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    latencyMetadata.addSubcommand(add(t, "LATENCY|DOCTOR",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes",
        "response_policy:special")),
      0, 0, 0, false));
    latencyMetadata.addSubcommand(
      add(t, "LATENCY|GRAPH", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes",
          "response_policy:special")),
        0, 0, 0, false));
    latencyMetadata.addSubcommand(add(t, "LATENCY|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    latencyMetadata.addSubcommand(add(t, "LATENCY|HISTOGRAM",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes",
        "response_policy:special")),
      0, 0, 0, false));
    latencyMetadata.addSubcommand(add(t, "LATENCY|HISTORY",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes",
        "response_policy:special")),
      0, 0, 0, false));
    latencyMetadata.addSubcommand(add(t, "LATENCY|LATEST",
      new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes",
        "response_policy:special")),
      0, 0, 0, false));
    latencyMetadata.addSubcommand(
      add(t, "LATENCY|RESET", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:agg_sum")), 0, 0,
        0, false));
    add(t, "LCS", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 2, 1, true);
    add(t, "LINDEX", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "LINSERT", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "LLEN", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "LMOVE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 2,
      1, true);
    add(t, "LMOVEM", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 2,
      1, true);
    add(t, "LMPOP", new HashSet<>(Arrays.asList("movablekeys", "write")), Collections.emptySet(), 0,
      0, 0, true);
    add(t, "LOLWUT", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 0, 0,
      0, false);
    add(t, "LPOP", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "LPOS", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1, true);
    add(t, "LPUSH", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "LPUSHX", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "LRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "LREM", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 1, 1, true);
    add(t, "LSET", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "LTRIM", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 1, 1, true);
    CommandMetadata memoryMetadata = add(t, "MEMORY", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    memoryMetadata.addSubcommand(add(t, "MEMORY|DOCTOR", Collections.emptySet(),
      new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_shards",
        "response_policy:special")),
      0, 0, 0, false));
    memoryMetadata.addSubcommand(add(t, "MEMORY|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    memoryMetadata.addSubcommand(add(t, "MEMORY|MALLOC-STATS", Collections.emptySet(),
      new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_shards",
        "response_policy:special")),
      0, 0, 0, false));
    memoryMetadata.addSubcommand(add(t, "MEMORY|PURGE", Collections.emptySet(),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")), 0,
      0, 0, false));
    memoryMetadata.addSubcommand(add(t, "MEMORY|STATS", Collections.emptySet(), new HashSet<>(Arrays
        .asList("nondeterministic_output", "request_policy:all_shards", "response_policy:special")),
      0, 0, 0, false));
    memoryMetadata.addSubcommand(add(t, "MEMORY|USAGE", new HashSet<>(Arrays.asList("readonly")),
      Collections.emptySet(), 2, 2, 1, true));
    add(t, "MGET", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard")), 1, -1, 1, true);
    add(t, "MIGRATE", new HashSet<>(Arrays.asList("movablekeys", "write")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 3, 3, 1, true);
    CommandMetadata moduleMetadata = add(t, "MODULE", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    moduleMetadata.addSubcommand(add(t, "MODULE|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    moduleMetadata
        .addSubcommand(add(t, "MODULE|LIST", new HashSet<>(Arrays.asList("admin", "noscript")),
          new HashSet<>(Arrays.asList("nondeterministic_output_order")), 0, 0, 0, false));
    moduleMetadata.addSubcommand(
      add(t, "MODULE|LOAD", new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript")),
        Collections.emptySet(), 0, 0, 0, false));
    moduleMetadata.addSubcommand(
      add(t, "MODULE|LOADEX", new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript")),
        Collections.emptySet(), 0, 0, 0, false));
    moduleMetadata.addSubcommand(
      add(t, "MODULE|UNLOAD", new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript")),
        Collections.emptySet(), 0, 0, 0, false));
    add(t, "MONITOR", new HashSet<>(Arrays.asList("admin", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "MOVE", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "MSET", new HashSet<>(Arrays.asList("denyoom", "write")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard", "response_policy:all_succeeded")),
      1, -1, 2, true);
    add(t, "MSETEX", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard", "response_policy:all_succeeded")),
      0, 0, 0, true);
    add(t, "MSETNX", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      -1, 2, true);
    add(t, "MULTI",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    CommandMetadata objectMetadata = add(t, "OBJECT", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    objectMetadata.addSubcommand(add(t, "OBJECT|ENCODING", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 2, 2, 1, true));
    objectMetadata.addSubcommand(add(t, "OBJECT|FREQ", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 2, 2, 1, true));
    objectMetadata.addSubcommand(add(t, "OBJECT|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    objectMetadata.addSubcommand(add(t, "OBJECT|IDLETIME", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 2, 2, 1, true));
    objectMetadata.addSubcommand(add(t, "OBJECT|REFCOUNT", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 2, 2, 1, true));
    add(t, "PERSIST", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "PEXPIRE", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "PEXPIREAT", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "PEXPIRETIME", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "PFADD", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "PFCOUNT", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, -1, 1,
      true);
    add(t, "PFDEBUG", new HashSet<>(Arrays.asList("admin", "denyoom", "write")),
      Collections.emptySet(), 2, 2, 1, true);
    add(t, "PFMERGE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      -1, 1, true);
    add(t, "PFSELFTEST", new HashSet<>(Arrays.asList("admin")), Collections.emptySet(), 0, 0, 0,
      false);
    add(t, "PING", new HashSet<>(Arrays.asList("fast")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:all_succeeded")), 0,
      0, 0, false);
    add(t, "PSETEX", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "PSUBSCRIBE",
      new HashSet<>(Arrays.asList("denyoom", "loading", "noscript", "pubsub", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "PSYNC",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "no_multi", "noscript")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "PTTL", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "PUBLISH", new HashSet<>(Arrays.asList("fast", "loading", "pubsub", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    CommandMetadata pubsubMetadata = add(t, "PUBSUB", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    pubsubMetadata.addSubcommand(
      add(t, "PUBSUB|CHANNELS", new HashSet<>(Arrays.asList("loading", "pubsub", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    pubsubMetadata.addSubcommand(add(t, "PUBSUB|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    pubsubMetadata.addSubcommand(
      add(t, "PUBSUB|NUMPAT", new HashSet<>(Arrays.asList("loading", "pubsub", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    pubsubMetadata.addSubcommand(
      add(t, "PUBSUB|NUMSUB", new HashSet<>(Arrays.asList("loading", "pubsub", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    pubsubMetadata.addSubcommand(
      add(t, "PUBSUB|SHARDCHANNELS", new HashSet<>(Arrays.asList("loading", "pubsub", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    pubsubMetadata.addSubcommand(
      add(t, "PUBSUB|SHARDNUMSUB", new HashSet<>(Arrays.asList("loading", "pubsub", "stale")),
        Collections.emptySet(), 0, 0, 0, false));
    add(t, "PUNSUBSCRIBE", new HashSet<>(Arrays.asList("loading", "noscript", "pubsub", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "QUIT",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "no_auth", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "RANDOMKEY", new HashSet<>(Arrays.asList("readonly")), new HashSet<>(Arrays
        .asList("nondeterministic_output", "request_policy:all_shards", "response_policy:special")),
      0, 0, 0, false);
    add(t, "READONLY", new HashSet<>(Arrays.asList("fast", "loading", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "READWRITE", new HashSet<>(Arrays.asList("fast", "loading", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "RENAME", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 2, 1, true);
    add(t, "RENAMENX", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 2,
      1, true);
    add(t, "REPLCONF",
      new HashSet<>(Arrays.asList("admin", "allow_busy", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "REPLICAOF",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "RESET",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "no_auth", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "RESTORE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "RESTORE-ASKING", new HashSet<>(Arrays.asList("asking", "denyoom", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ROLE", new HashSet<>(Arrays.asList("fast", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "RPOP", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "RPOPLPUSH", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      2, 1, true);
    add(t, "RPUSH", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "RPUSHX", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "SADD", new HashSet<>(Arrays.asList("denyoom", "fast", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "SAVE",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "no_multi", "noscript")),
      Collections.emptySet(), 0, 0, 0, false);
    add(
      t, "SCAN", new HashSet<>(Arrays.asList("readonly")), new HashSet<>(Arrays
          .asList("nondeterministic_output", "request_policy:special", "response_policy:special")),
      0, 0, 0, false);
    add(t, "SCARD", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    CommandMetadata scriptMetadata = add(t, "SCRIPT", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    scriptMetadata.addSubcommand(add(t, "SCRIPT|DEBUG", new HashSet<>(Arrays.asList("noscript")),
      Collections.emptySet(), 0, 0, 0, false));
    scriptMetadata.addSubcommand(add(t, "SCRIPT|EXISTS", new HashSet<>(Arrays.asList("noscript")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:agg_logical_and")),
      0, 0, 0, false));
    scriptMetadata.addSubcommand(add(t, "SCRIPT|FLUSH", new HashSet<>(Arrays.asList("noscript")),
      new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")), 0,
      0, 0, false));
    scriptMetadata.addSubcommand(add(t, "SCRIPT|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    scriptMetadata.addSubcommand(
      add(t, "SCRIPT|KILL", new HashSet<>(Arrays.asList("allow_busy", "noscript")),
        new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:one_succeeded")),
        0, 0, 0, false));
    scriptMetadata
        .addSubcommand(add(t, "SCRIPT|LOAD", new HashSet<>(Arrays.asList("noscript", "stale")),
          new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
          0, 0, 0, false));
    add(t, "SDIFF", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, -1, 1, true);
    add(t, "SDIFFCARD", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "SDIFFSTORE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(),
      1, -1, 1, true);
    add(t, "SEARCH.CLUSTERINFO",
      new HashSet<>(Arrays.asList("loading", "module", "noscript", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "SEARCH.CLUSTERREFRESH", new HashSet<>(Arrays.asList("module", "noscript", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "SEARCH.CLUSTERSET",
      new HashSet<>(Arrays.asList("loading", "module", "noscript", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "SELECT", new HashSet<>(Arrays.asList("fast", "loading", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "SET", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "SETBIT", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "SETEX", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "SETNX", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "SETRANGE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "SHUTDOWN",
      new HashSet<>(
          Arrays.asList("admin", "allow_busy", "loading", "no_multi", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "SINTER", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, -1, 1, true);
    add(t, "SINTERCARD", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "SINTERSTORE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(),
      1, -1, 1, true);
    add(t, "SISMEMBER", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "SLAVEOF",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    CommandMetadata slowlogMetadata = add(t, "SLOWLOG", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    slowlogMetadata.addSubcommand(
      add(t, "SLOWLOG|GET", new HashSet<>(Arrays.asList("admin", "loading", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes")), 0, 0,
        0, false));
    slowlogMetadata.addSubcommand(add(t, "SLOWLOG|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    slowlogMetadata.addSubcommand(
      add(t, "SLOWLOG|LEN", new HashSet<>(Arrays.asList("admin", "loading", "stale")),
        new HashSet<>(Arrays.asList("nondeterministic_output", "request_policy:all_nodes",
          "response_policy:agg_sum")),
        0, 0, 0, false));
    slowlogMetadata.addSubcommand(
      add(t, "SLOWLOG|RESET", new HashSet<>(Arrays.asList("admin", "loading", "stale")),
        new HashSet<>(Arrays.asList("request_policy:all_nodes", "response_policy:all_succeeded")),
        0, 0, 0, false));
    add(t, "SMEMBERS", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, 1, 1, true);
    add(t, "SMISMEMBER", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "SMOVE", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 2, 1,
      true);
    add(t, "SORT", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "SORT_RO", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "SPOP", new HashSet<>(Arrays.asList("fast", "write")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "SPUBLISH", new HashSet<>(Arrays.asList("fast", "loading", "pubsub", "stale")),
      Collections.emptySet(), 1, 1, 1, false);
    add(t, "SRANDMEMBER", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "SREM", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "SSCAN", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "SSUBSCRIBE",
      new HashSet<>(Arrays.asList("denyoom", "loading", "noscript", "pubsub", "stale")),
      Collections.emptySet(), 1, -1, 1, false);
    add(t, "STRLEN", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "SUBSCRIBE",
      new HashSet<>(Arrays.asList("denyoom", "loading", "noscript", "pubsub", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "SUBSTR", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "SUNION", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output_order")), 1, -1, 1, true);
    add(t, "SUNIONCARD", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "SUNIONSTORE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(),
      1, -1, 1, true);
    add(t, "SUNSUBSCRIBE", new HashSet<>(Arrays.asList("loading", "noscript", "pubsub", "stale")),
      Collections.emptySet(), 1, -1, 1, false);
    add(t, "SWAPDB", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 0, 0, 0,
      false);
    add(t, "SYNC",
      new HashSet<>(Arrays.asList("admin", "no_async_loading", "no_multi", "noscript")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "TDIGEST.ADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.BYRANK", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.BYREVRANK", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.CDF", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.CREATE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.INFO", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "TDIGEST.MAX", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.MERGE",
      new HashSet<>(Arrays.asList("denyoom", "module", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.MIN", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.QUANTILE", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.RANK", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.RESET", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.REVRANK", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TDIGEST.TRIMMED_MEAN", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TIME", new HashSet<>(Arrays.asList("fast", "loading", "stale")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 0, 0, 0, false);
    add(t, "TIMESERIES.CLUSTERSET", new HashSet<>(Arrays.asList("module", "noscript", "readonly")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "TIMESERIES.REFRESHCLUSTER",
      new HashSet<>(Arrays.asList("module", "noscript", "readonly")), Collections.emptySet(), 0, 0,
      0, false);
    add(t, "TOPK.ADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TOPK.COUNT", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "TOPK.INCRBY", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TOPK.INFO", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "TOPK.LIST", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "TOPK.QUERY", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "TOPK.RESERVE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TOUCH", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard", "response_policy:agg_sum")), 1, -1,
      1, true);
    add(t, "TRIMSLOTS", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 0, 0, 0,
      false);
    add(t, "TS.ADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TS.ALTER", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TS.CREATE", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TS.CREATERULE", new HashSet<>(Arrays.asList("fast", "module", "write")),
      Collections.emptySet(), 1, 2, 1, true);
    add(t, "TS.DECRBY", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
  }

  private static void init2(Map<String, CommandMetadata> t) {
    add(t, "TS.DEL", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "TS.DELETERULE", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(),
      1, 2, 1, true);
    add(t, "TS.GET", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "TS.INCRBY", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TS.INFO", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "TS.MADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, -1, 3, true);
    add(t, "TS.MGET", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "TS.MRANGE", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "TS.MREVRANGE", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "TS.NRANGE", new HashSet<>(Arrays.asList("module", "movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "TS.NREVRANGE", new HashSet<>(Arrays.asList("module", "movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "TS.QUERYINDEX", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "TS.QUERYLABELS", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false);
    add(t, "TS.RANGE", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "TS.READ", new HashSet<>(Arrays.asList("module", "readonly")),
      new HashSet<>(Arrays.asList("dont_cache")), 1, 1, 1, true);
    add(t, "TS.REVRANGE", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "TTL", new HashSet<>(Arrays.asList("fast", "readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "TYPE", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "UNLINK", new HashSet<>(Arrays.asList("fast", "write")),
      new HashSet<>(Arrays.asList("request_policy:multi_shard", "response_policy:agg_sum")), 1, -1,
      1, true);
    add(t, "UNSUBSCRIBE", new HashSet<>(Arrays.asList("loading", "noscript", "pubsub", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "UNWATCH",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "noscript", "stale")),
      Collections.emptySet(), 0, 0, 0, false);
    add(t, "VADD", new HashSet<>(Arrays.asList("denyoom", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VCARD", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VDIM", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VEMB", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VGETATTR", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VINFO", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VISMEMBER", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "VLINKS", new HashSet<>(Arrays.asList("fast", "module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VRANDMEMBER", new HashSet<>(Arrays.asList("module", "readonly")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VRANGE", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "VREM", new HashSet<>(Arrays.asList("module", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "VSETATTR", new HashSet<>(Arrays.asList("fast", "module", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "VSIM", new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "WAIT", new HashSet<>(Arrays.asList("blocking")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:agg_min")), 0, 0, 0,
      false);
    add(t, "WAITAOF", new HashSet<>(Arrays.asList("blocking")),
      new HashSet<>(Arrays.asList("request_policy:all_shards", "response_policy:agg_min")), 0, 0, 0,
      false);
    add(t, "WATCH",
      new HashSet<>(Arrays.asList("allow_busy", "fast", "loading", "noscript", "stale")),
      Collections.emptySet(), 1, -1, 1, true);
    add(t, "XACK", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "XACKDEL", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "XADD", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "XAUTOCLAIM", new HashSet<>(Arrays.asList("fast", "write")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "XCFGSET", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "XCLAIM", new HashSet<>(Arrays.asList("fast", "write")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "XDEL", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "XDELEX", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    CommandMetadata xgroupMetadata = add(t, "XGROUP", Collections.emptySet(),
      Collections.emptySet(), 0, 0, 0, false);
    xgroupMetadata.addSubcommand(add(t, "XGROUP|CREATE",
      new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 2, 2, 1, true));
    xgroupMetadata.addSubcommand(add(t, "XGROUP|CREATECONSUMER",
      new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(), 2, 2, 1, true));
    xgroupMetadata.addSubcommand(add(t, "XGROUP|DELCONSUMER", new HashSet<>(Arrays.asList("write")),
      Collections.emptySet(), 2, 2, 1, true));
    xgroupMetadata.addSubcommand(add(t, "XGROUP|DESTROY", new HashSet<>(Arrays.asList("write")),
      Collections.emptySet(), 2, 2, 1, true));
    xgroupMetadata.addSubcommand(add(t, "XGROUP|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    xgroupMetadata.addSubcommand(add(t, "XGROUP|SETID", new HashSet<>(Arrays.asList("write")),
      Collections.emptySet(), 2, 2, 1, true));
    add(t, "XIDMPRECORD", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    CommandMetadata xinfoMetadata = add(t, "XINFO", Collections.emptySet(), Collections.emptySet(),
      0, 0, 0, false);
    xinfoMetadata.addSubcommand(add(t, "XINFO|CONSUMERS", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 2, 2, 1, true));
    xinfoMetadata.addSubcommand(add(t, "XINFO|GROUPS", new HashSet<>(Arrays.asList("readonly")),
      Collections.emptySet(), 2, 2, 1, true));
    xinfoMetadata.addSubcommand(add(t, "XINFO|HELP",
      new HashSet<>(Arrays.asList("loading", "stale")), Collections.emptySet(), 0, 0, 0, false));
    xinfoMetadata.addSubcommand(add(t, "XINFO|STREAM", new HashSet<>(Arrays.asList("readonly")),
      Collections.emptySet(), 2, 2, 1, true));
    add(t, "XLEN", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "XNACK", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "XPENDING", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "XRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "XREAD", new HashSet<>(Arrays.asList("blocking", "movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "XREADGROUP", new HashSet<>(Arrays.asList("blocking", "movablekeys", "write")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "XREVRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "XSETID", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "XTRIM", new HashSet<>(Arrays.asList("write")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "ZADD", new HashSet<>(Arrays.asList("denyoom", "fast", "write")), Collections.emptySet(),
      1, 1, 1, true);
    add(t, "ZCARD", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZCOUNT", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZDIFF", new HashSet<>(Arrays.asList("movablekeys", "readonly")), Collections.emptySet(),
      0, 0, 0, true);
    add(t, "ZDIFFSTORE", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ZINCRBY", new HashSet<>(Arrays.asList("denyoom", "fast", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ZINTER", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "ZINTERCARD", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "ZINTERSTORE", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    add(t, "ZLEXCOUNT", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "ZMPOP", new HashSet<>(Arrays.asList("movablekeys", "write")), Collections.emptySet(), 0,
      0, 0, true);
    add(t, "ZMSCORE", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "ZPOPMAX", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZPOPMIN", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZRANDMEMBER", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "ZRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ZRANGEBYLEX", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ZRANGEBYSCORE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZRANGESTORE", new HashSet<>(Arrays.asList("denyoom", "write")), Collections.emptySet(),
      1, 2, 1, true);
    add(t, "ZRANK", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZREM", new HashSet<>(Arrays.asList("fast", "write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ZREMRANGEBYLEX", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ZREMRANGEBYRANK", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZREMRANGEBYSCORE", new HashSet<>(Arrays.asList("write")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZREVRANGE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1, 1,
      true);
    add(t, "ZREVRANGEBYLEX", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZREVRANGEBYSCORE", new HashSet<>(Arrays.asList("readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "ZREVRANK", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1,
      1, 1, true);
    add(t, "ZSCAN", new HashSet<>(Arrays.asList("readonly")),
      new HashSet<>(Arrays.asList("nondeterministic_output")), 1, 1, 1, true);
    add(t, "ZSCORE", new HashSet<>(Arrays.asList("fast", "readonly")), Collections.emptySet(), 1, 1,
      1, true);
    add(t, "ZUNION", new HashSet<>(Arrays.asList("movablekeys", "readonly")),
      Collections.emptySet(), 0, 0, 0, true);
    add(t, "ZUNIONSTORE", new HashSet<>(Arrays.asList("denyoom", "movablekeys", "write")),
      Collections.emptySet(), 1, 1, 1, true);
    CommandMetadata _ftConfigMetadata = add(t, "_FT.CONFIG",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false);
    _ftConfigMetadata
        .addSubcommand(add(t, "_FT.CONFIG|GET", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    _ftConfigMetadata
        .addSubcommand(add(t, "_FT.CONFIG|HELP", new HashSet<>(Arrays.asList("module", "readonly")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    _ftConfigMetadata
        .addSubcommand(add(t, "_FT.CONFIG|SET", new HashSet<>(Arrays.asList("module", "write")),
          new HashSet<>(Arrays.asList("dont_cache")), 0, 0, 0, false));
    CommandMetadata _ftDebugMetadata = add(t, "_FT.DEBUG",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false);
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|BG_SCAN_CONTROLLER",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|CLEAR_PENDING_TOPOLOGY",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|COORD_THREADS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DELETE_LOCAL_COORD_CURSORS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DELETE_LOCAL_CURSORS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DISK_FLUSH",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DISK_IO_CONTROL",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DOCIDTOID",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DOCINFO",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_DELETED_IDS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_GEOMIDX",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_HNSW",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_INVIDX",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_NUMIDX",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_NUMIDXTREE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_PHONETIC_HASH",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_PREFIX_TRIE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_SCHEMA",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_SUFFIX_TRIE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_TAGIDX",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|DUMP_TERMS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|FT.AGGREGATE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|FT.HYBRID",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|FT.PROFILE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|FT.SEARCH",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GC_CLEAN_NUMERIC",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GC_CONTINUE_SCHEDULE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GC_FORCEBGINVOKE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GC_FORCEINVOKE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GC_STOP_SCHEDULE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GC_WAIT_FOR_JOBS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GET_HIDE_USER_DATA_FROM_LOGS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GET_MAX_DOC_ID",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|GIT_SHA",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|HELP",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|IDTODOCID",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|INDEXER_SLEEP_BEFORE_YIELD_MICROS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|INDEXES",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|INFO",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|INFO_TAGIDX",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|INVIDX_SUMMARY",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|NUMIDX_SUMMARY",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|PAUSE_TOPOLOGY_UPDATER",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|QUERY_CONTROLLER",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|REGISTER_TEST_SCORERS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|RESUME_TOPOLOGY_UPDATER",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|SET_MAX_INDEXES",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|SET_MONITOR_EXPIRATION",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|SHARD_CONNECTION_STATES",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|SPEC_INVIDXES_INFO",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|TTL",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|TTL_EXPIRE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|TTL_PAUSE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|VECSIM_INFO",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|VECSIM_MOCK_TIMEOUT",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|WORKERS",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|YIELDS_COUNTER",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|_FT.AGGREGATE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|_FT.HYBRID",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|_FT.PROFILE",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
    _ftDebugMetadata.addSubcommand(add(t, "_FT.DEBUG|_FT.SEARCH",
      new HashSet<>(Arrays.asList("module", "readonly")), Collections.emptySet(), 0, 0, 0, false));
  }

  private static void registerAll(Map<String, CommandMetadata> t) {
    init0(t);
    init1(t);
    init2(t);
  }
  // GENERATED-METADATA-END
}
