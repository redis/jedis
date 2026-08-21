package redis.clients.jedis;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import redis.clients.jedis.CommandFlagsRegistry.CommandFlag;
import redis.clients.jedis.CommandFlagsRegistry.RequestPolicy;
import redis.clients.jedis.CommandFlagsRegistry.ResponsePolicy;
import redis.clients.jedis.MetadataResolver.CommandMetadata;

/**
 * Populates the {@link StaticCommandFlagsRegistry} from the shared command metadata of
 * {@link MetadataResolver}, replacing the previously generated initializer so command flags and
 * request/response policies come from the same single metadata source as cacheability.
 * <p>
 * Two adjustments are applied on top of the raw metadata, each defined declaratively below:
 * {@link #MANUAL_OVERRIDES} (narrow per-command replacements of specific attributes) and
 * {@link #FLAGS_BY_NAME} (the closed {@code CommandFlag} enum; unknown metadata flags are skipped).
 * Both encode library capabilities, not metadata truths: corrections to the metadata itself belong
 * in {@link MetadataResolver}, where every metadata consumer sees them.
 */
final class CommandFlagsInitializer {

  /**
   * Manual command overrides that take precedence over the resolved server metadata. An override
   * replaces only the attributes it sets; {@code null} attributes are inherited from the metadata,
   * keeping overrides narrow and immune to future metadata changes they do not mean to mask.
   * <p>
   * Key: full command name, {@code PARENT|CHILD} for subcommands. To add a new override, add an
   * entry to this map in the static initializer block; remove it once the underlying limitation is
   * fixed.
   */
  private static final Map<String, ManualOverride> MANUAL_OVERRIDES = new LinkedHashMap<>();
  static {
    // Override INFO: change request policy from ALL_SHARDS to DEFAULT
    // Reason: SPECIAL response policy not yet supported in the library and defaults to return
    // single result. INFO should be executed on a single node, not broadcast to all shards.
    MANUAL_OVERRIDES.put("INFO", new ManualOverride(null, RequestPolicy.DEFAULT, null));

    // Override FUNCTION STATS: change request policy from ALL_SHARDS to DEFAULT
    // Reason: SPECIAL response policy not yet supported in the library and defaults to return
    // single result. FUNCTION STATS should be executed on a single node, not broadcast to all
    // shards.
    MANUAL_OVERRIDES.put("FUNCTION|STATS", new ManualOverride(null, RequestPolicy.DEFAULT, null));
  }

  /**
   * Metadata flag names mapped to the closed {@link CommandFlag} enum. Derived from the enum itself
   * so the mapping cannot drift when the enum changes. Metadata flags without an enum constant (for
   * example {@code script_runner}) are skipped, matching the previous generator.
   */
  private static final Map<String, CommandFlag> FLAGS_BY_NAME = new HashMap<>(64);
  private static final Map<String, RequestPolicy> REQUEST_POLICIES_BY_NAME = new HashMap<>(16);
  private static final Map<String, ResponsePolicy> RESPONSE_POLICIES_BY_NAME = new HashMap<>(16);
  static {
    for (CommandFlag flag : CommandFlag.values()) {
      FLAGS_BY_NAME.put(flag.name().toLowerCase(), flag);
    }
    for (RequestPolicy policy : RequestPolicy.values()) {
      REQUEST_POLICIES_BY_NAME.put("request_policy:" + policy.name().toLowerCase(), policy);
    }
    for (ResponsePolicy policy : ResponsePolicy.values()) {
      RESPONSE_POLICIES_BY_NAME.put("response_policy:" + policy.name().toLowerCase(), policy);
    }
  }

  private CommandFlagsInitializer() {
  }

  /** Registers every command and container subcommand known to the shared metadata. */
  public static void initialize(StaticCommandFlagsRegistry.Builder builder) {
    for (CommandMetadata metadata : MetadataResolver.commandTable().values()) {
      String name = metadata.getName();
      if (name.indexOf('|') >= 0) {
        continue; // registered through its parent below, keeping the hierarchy
      }
      register(builder, name, null, metadata);
      for (CommandMetadata subcommand : metadata.getSubcommands()) {
        register(builder, name, subcommand.getName().substring(name.length() + 1), subcommand);
      }
    }
  }

  private static void register(StaticCommandFlagsRegistry.Builder builder, String name,
      String subcommand, CommandMetadata metadata) {
    ManualOverride override = MANUAL_OVERRIDES.get(metadata.getName());
    EnumSet<CommandFlag> flags = override != null && override.flags != null ? override.flags
        : mapFlags(metadata);
    RequestPolicy requestPolicy = override != null && override.requestPolicy != null
        ? override.requestPolicy
        : mapPolicy(metadata, REQUEST_POLICIES_BY_NAME);
    ResponsePolicy responsePolicy = override != null && override.responsePolicy != null
        ? override.responsePolicy
        : mapPolicy(metadata, RESPONSE_POLICIES_BY_NAME);
    if (requestPolicy == null && responsePolicy == null) {
      if (subcommand == null) {
        builder.register(name, flags);
      } else {
        builder.register(name, subcommand, flags);
      }
    } else {
      RequestPolicy request = requestPolicy != null ? requestPolicy : RequestPolicy.DEFAULT;
      ResponsePolicy response = responsePolicy != null ? responsePolicy : ResponsePolicy.DEFAULT;
      if (subcommand == null) {
        builder.register(name, flags, request, response);
      } else {
        builder.register(name, subcommand, flags, request, response);
      }
    }
  }

  private static EnumSet<CommandFlag> mapFlags(CommandMetadata metadata) {
    EnumSet<CommandFlag> flags = StaticCommandFlagsRegistry.EMPTY_FLAGS;
    for (String flag : metadata.getFlags()) {
      CommandFlag mapped = FLAGS_BY_NAME.get(flag);
      if (mapped != null) {
        if (flags == StaticCommandFlagsRegistry.EMPTY_FLAGS) {
          flags = EnumSet.noneOf(CommandFlag.class);
        }
        flags.add(mapped);
      }
    }
    return flags;
  }

  private static <P> P mapPolicy(CommandMetadata metadata, Map<String, P> mapping) {
    for (String tip : metadata.getTips()) {
      P policy = mapping.get(tip);
      if (policy != null) {
        return policy;
      }
    }
    return null;
  }

  /** A narrow manual registration: only non-null attributes replace the metadata-derived ones. */
  private static final class ManualOverride {

    final EnumSet<CommandFlag> flags;
    final RequestPolicy requestPolicy;
    final ResponsePolicy responsePolicy;

    ManualOverride(EnumSet<CommandFlag> flags, RequestPolicy requestPolicy,
        ResponsePolicy responsePolicy) {
      this.flags = flags;
      this.requestPolicy = requestPolicy;
      this.responsePolicy = responsePolicy;
    }
  }
}
