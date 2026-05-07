package redis.clients.jedis.codegen;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Module;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.resps.CommandInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Code generator for StaticCommandFlagsRegistry. This generator connects to a Redis server,
 * retrieves all command metadata using the COMMAND command, and automatically generates the
 * StaticCommandFlagsRegistry class that implements CommandFlagsRegistry interface.
 * <p>
 * Usage:
 *
 * <pre>
 * java -cp ... redis.clients.jedis.codegen.CommandFlagsRegistryGenerator [host] [port]
 * </pre>
 * <p>
 * Arguments:
 * <ul>
 * <li>host - Redis server hostname (default: localhost)</li>
 * <li>port - Redis server port (default: 6379)</li>
 * </ul>
 * <p>
 * Note: This is a code generation tool and should NOT be executed as part of regular tests.
 */
public class CommandFlagsRegistryGenerator {

  private static final String JAVA_FILE = "src/main/java/redis/clients/jedis/StaticCommandFlagsRegistryInitializer.java";
  private static final String BACKUP_JSON_FILE = "redis_commands_metadata.json";

  private final String redisHost;
  private final int redisPort;

  // Server metadata collected during generation
  private ServerMetadata serverMetadata;

  // Map JSON flag names to Java enum names
  private static final Map<String, String> FLAG_MAPPING = new LinkedHashMap<>();
  static {
    FLAG_MAPPING.put("readonly", "READONLY");
    FLAG_MAPPING.put("write", "WRITE");
    FLAG_MAPPING.put("denyoom", "DENYOOM");
    FLAG_MAPPING.put("admin", "ADMIN");
    FLAG_MAPPING.put("pubsub", "PUBSUB");
    FLAG_MAPPING.put("noscript", "NOSCRIPT");
    FLAG_MAPPING.put("random", "RANDOM");
    FLAG_MAPPING.put("sort_for_script", "SORT_FOR_SCRIPT");
    FLAG_MAPPING.put("loading", "LOADING");
    FLAG_MAPPING.put("stale", "STALE");
    FLAG_MAPPING.put("skip_monitor", "SKIP_MONITOR");
    FLAG_MAPPING.put("skip_slowlog", "SKIP_SLOWLOG");
    FLAG_MAPPING.put("asking", "ASKING");
    FLAG_MAPPING.put("fast", "FAST");
    FLAG_MAPPING.put("movablekeys", "MOVABLEKEYS");
    FLAG_MAPPING.put("module", "MODULE");
    FLAG_MAPPING.put("blocking", "BLOCKING");
    FLAG_MAPPING.put("no_auth", "NO_AUTH");
    FLAG_MAPPING.put("no_async_loading", "NO_ASYNC_LOADING");
    FLAG_MAPPING.put("no_multi", "NO_MULTI");
    FLAG_MAPPING.put("no_mandatory_keys", "NO_MANDATORY_KEYS");
    FLAG_MAPPING.put("allow_busy", "ALLOW_BUSY");
  }

  /**
   * Manual command flag overrides that take precedence over Redis server metadata. These overrides
   * allow defining custom flag combinations, request policies, and response policies for specific
   * commands when the server-provided metadata is incorrect or needs customization.
   * <p>
   * Key: Command name (uppercase, e.g., "KEYS" or "ACL CAT" for subcommands) Value: CommandMetadata
   * with the override values
   * <p>
   * To add a new override, add an entry to this map in the static initializer block.
   */
  private static final Map<String, CommandMetadata> MANUAL_OVERRIDES = new LinkedHashMap<>();
  static {
    // Override INFO: change request policy from ALL_SHARDS to DEFAULT
    // Reason: SPECIAL response policy not yet supported in the library and defaults to return
    // single result INFO should be executed on a single node, not broadcast to all shards
    MANUAL_OVERRIDES.put("INFO",
      new CommandMetadata(Arrays.asList("loading", "stale"), "default", "special"));

    // Override FUNCTION STATS: change request policy from ALL_SHARDS to DEFAULT
    // Reason: SPECIAL response policy not yet supported in the library and defaults to return
    // single result FUNCTION STATS should be executed on a single node, not broadcast to all shards
    MANUAL_OVERRIDES.put("FUNCTION STATS",
      new CommandMetadata(Arrays.asList("noscript", "allow_busy"), "default", "special"));
  }

  /**
   * Known parent commands that have subcommands. When the generator encounters a command name
   * containing a space (e.g., "FUNCTION LOAD"), the parent part (e.g., "FUNCTION") must be listed
   * here for proper subcommand registration.
   * <p>
   * If a new Redis command with subcommands is added, add the parent command name to this set. The
   * generator will fail with a helpful error message if an unknown parent command is encountered.
   */
  private static final Set<String> KNOWN_PARENT_COMMANDS = new HashSet<>(Arrays.asList("ACL",
    "CLIENT", "CLUSTER", "COMMAND", "CONFIG", "FUNCTION", "HOTKEYS", "LATENCY", "MEMORY", "MODULE",
    "OBJECT", "PUBSUB", "SCRIPT", "SLOWLOG", "XGROUP", "XINFO", "FT.CONFIG", "FT.CURSOR"));

  public CommandFlagsRegistryGenerator(String host, int port) {
    this.redisHost = host;
    this.redisPort = port;
  }

  public static void main(String[] args) {
    printLine();
    System.out.println("StaticCommandFlagsRegistry Generator");
    printLine();

    // Parse command line arguments
    String host = args.length > 0 ? args[0] : "localhost";
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 6379;

    System.out.println("Redis server: " + host + ":" + port);
    System.out.println();

    try {
      CommandFlagsRegistryGenerator generator = new CommandFlagsRegistryGenerator(host, port);
      generator.generate();

      System.out.println();
      printLine();
      System.out.println("✓ Code generation completed successfully!");
      printLine();
    } catch (Exception e) {
      System.err.println();
      printLine();
      System.err.println("✗ Code generation failed!");
      printLine();
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printLine() {
    for (int i = 0; i < 80; i++) {
      System.out.print("=");
    }
    System.out.println();
  }

  public void generate() throws IOException {
    Map<String, CommandMetadata> commandsMetadata;

    // Step 1: Retrieve commands from Redis
    System.out.println("\nStep 1: Connecting to Redis at " + redisHost + ":" + redisPort + "...");
    try {
      commandsMetadata = retrieveCommandsFromRedis();
      System.out.println("✓ Retrieved " + commandsMetadata.size() + " commands from Redis");

      // Save to backup JSON file
      saveToJsonFile(commandsMetadata);
    } catch (JedisConnectionException e) {
      System.err.println("✗ Failed to connect to Redis: " + e.getMessage());
      System.out.println("\nAttempting to use backup JSON file: " + BACKUP_JSON_FILE);
      commandsMetadata = readJsonFile();
      System.out.println("✓ Loaded " + commandsMetadata.size() + " commands from backup file");
    }

    // Step 2: Process commands and group by metadata combinations
    System.out.println("\nStep 2: Processing commands and grouping by metadata...");
    Map<MetadataKey, List<String>> metadataCombinations = groupByMetadata(commandsMetadata);
    System.out.println("✓ Found " + metadataCombinations.size() + " unique metadata combinations");

    // Step 3: Generate StaticCommandFlagsRegistry class
    System.out.println("\nStep 3: Generating StaticCommandFlagsRegistry class...");
    String classContent = generateRegistryClass(metadataCombinations);
    System.out.println("✓ Generated " + classContent.split("\n").length + " lines of code");

    // Step 4: Write StaticCommandFlagsRegistry.java
    System.out.println("\nStep 4: Writing " + JAVA_FILE + "...");
    writeJavaFile(classContent);
    System.out.println("✓ Successfully created StaticCommandFlagsRegistry.java");
  }

  private Map<String, CommandMetadata> retrieveCommandsFromRedis() {
    Map<String, CommandMetadata> result = new LinkedHashMap<>();

    try (Jedis jedis = new Jedis(redisHost, redisPort)) {

      jedis.connect();
      jedis.auth(Endpoints.getRedisEndpoint("standalone0").getPassword());

      // Collect server metadata
      String infoServer = jedis.info("server");
      String version = extractInfoValue(infoServer, "redis_version");
      String mode = extractInfoValue(infoServer, "redis_mode");

      // Get loaded modules
      List<String> modules = new ArrayList<>();
      try {
        List<Module> moduleList = jedis.moduleList();
        for (Module module : moduleList) {
          modules.add(module.getName());
        }
      } catch (Exception e) {
        // Module list might not be available in all Redis versions
        System.out.println("  Note: Could not retrieve module list: " + e.getMessage());
      }

      serverMetadata = new ServerMetadata(version, mode, modules);

      // Get all commands using COMMAND
      Map<String, CommandInfo> commands = jedis.command();

      for (Map.Entry<String, CommandInfo> entry : commands.entrySet()) {
        CommandInfo cmdInfo = entry.getValue();
        String commandName = normalizeCommandName(cmdInfo.getName());

        // Check for subcommands
        Map<String, CommandInfo> subcommands = cmdInfo.getSubcommands();

        if (subcommands != null && !subcommands.isEmpty()) {
          // This command has subcommands - add parent command with its flags first
          if (!shouldExcludeCommand(commandName)) {
            CommandMetadata parentMetadata = extractCommandMetadata(cmdInfo);
            result.put(commandName, parentMetadata);
          }

          // Then process subcommands
          for (Map.Entry<String, CommandInfo> subEntry : subcommands.entrySet()) {
            CommandInfo subCmdInfo = subEntry.getValue();
            String subCommandName = normalizeCommandName(subCmdInfo.getName());

            // Filter out unwanted commands
            if (shouldExcludeCommand(subCommandName)) {
              continue;
            }

            CommandMetadata metadata = extractCommandMetadata(subCmdInfo);
            result.put(subCommandName, metadata);
          }
        } else {
          // Regular command without subcommands
          // Filter out unwanted commands
          if (!shouldExcludeCommand(commandName)) {
            CommandMetadata metadata = extractCommandMetadata(cmdInfo);
            result.put(commandName, metadata);
          }
        }
      }

    }
    // Ignore close errors

    return result;
  }

  /**
   * Extract command metadata (flags, request_policy, response_policy) from CommandInfo.
   */
  private CommandMetadata extractCommandMetadata(CommandInfo cmdInfo) {
    // Get flags
    List<String> flags = new ArrayList<>();
    if (cmdInfo.getFlags() != null) {
      for (String flag : cmdInfo.getFlags()) {
        flags.add(flag.toLowerCase());
      }
    }

    // Extract request_policy and response_policy from tips
    String requestPolicy = null;
    String responsePolicy = null;
    List<String> tips = cmdInfo.getTips();
    if (tips != null) {
      for (String tip : tips) {
        String tipLower = tip.toLowerCase();
        if (tipLower.startsWith("request_policy:")) {
          requestPolicy = tipLower.substring("request_policy:".length());
        } else if (tipLower.startsWith("response_policy:")) {
          responsePolicy = tipLower.substring("response_policy:".length());
        }
      }
    }

    return new CommandMetadata(flags, requestPolicy, responsePolicy);
  }

  /**
   * Normalize command name: replace pipe separators with spaces and convert to uppercase. Redis
   * returns command names like "acl|help" but Jedis uses "ACL HELP".
   */
  private String normalizeCommandName(String commandName) {
    return commandName.replace('|', ' ').toUpperCase();
  }

  /**
   * Check if a command should be excluded from the registry.
   * <p>
   * Exclusion rules:
   * <ul>
   * <li>All HELP subcommands (e.g., "ACL HELP", "CONFIG HELP", "XINFO HELP")</li>
   * <li>All FT.DEBUG subcommands (e.g., "FT.DEBUG DUMP_TERMS", "FT.DEBUG GIT_SHA")</li>
   * <li>All _FT.DEBUG subcommands (internal RediSearch debug commands)</li>
   * </ul>
   */
  private boolean shouldExcludeCommand(String commandName) {
    // Exclude all HELP subcommands
    if (commandName.endsWith(" HELP")) {
      return true;
    }

    // Exclude FT.DEBUG and _FT.DEBUG subcommands
    return commandName.startsWith("FT.DEBUG ") || commandName.startsWith("_FT.DEBUG ")
        || commandName.startsWith("_FT.CONFIG ");
  }

  private String extractInfoValue(String info, String key) {
    String[] lines = info.split("\n");
    for (String line : lines) {
      if (line.startsWith(key + ":")) {
        return line.substring(key.length() + 1).trim();
      }
    }
    return "unknown";
  }

  private void saveToJsonFile(Map<String, CommandMetadata> commandsMetadata) throws IOException {
    Gson gson = new Gson();
    String json = gson.toJson(commandsMetadata);

    Path jsonPath = Paths.get(BACKUP_JSON_FILE);
    Files.write(jsonPath, json.getBytes(StandardCharsets.UTF_8));
    System.out.println("✓ Saved backup to " + BACKUP_JSON_FILE);
  }

  private Map<String, CommandMetadata> readJsonFile() throws IOException {
    Path jsonPath = Paths.get(BACKUP_JSON_FILE);
    if (!Files.exists(jsonPath)) {
      throw new IOException("Backup file not found: " + BACKUP_JSON_FILE);
    }

    // JDK 8 compatible: read file as bytes and convert to string
    byte[] bytes = Files.readAllBytes(jsonPath);
    String jsonContent = new String(bytes, StandardCharsets.UTF_8);

    Gson gson = new Gson();

    // Parse JSON with proper type
    Type type = new TypeToken<Map<String, CommandMetadata>>() {
    }.getType();
    Map<String, CommandMetadata> parsed = gson.fromJson(jsonContent, type);

    return new LinkedHashMap<>(parsed);
  }

  private Map<MetadataKey, List<String>> groupByMetadata(
      Map<String, CommandMetadata> commandsMetadata) {
    Map<MetadataKey, List<String>> result = new LinkedHashMap<>();

    for (Map.Entry<String, CommandMetadata> entry : commandsMetadata.entrySet()) {
      String command = entry.getKey();
      String commandUpper = command.toUpperCase();

      // Check for manual override first - overrides take precedence over server metadata
      CommandMetadata metadata;
      if (MANUAL_OVERRIDES.containsKey(commandUpper)) {
        metadata = MANUAL_OVERRIDES.get(commandUpper);
        System.out.println("  Applying manual override for command: " + commandUpper);
      } else {
        metadata = entry.getValue();
      }

      // Convert JSON flags to Java enum names and sort
      List<String> javaFlags = metadata.flags.stream().map(f -> FLAG_MAPPING.get(f.toLowerCase()))
          .filter(Objects::nonNull).sorted().collect(Collectors.toList());

      // Convert request and response policies to Java enum names (uppercase)
      String requestPolicy = metadata.requestPolicy != null ? metadata.requestPolicy.toUpperCase()
          : null;
      String responsePolicy = metadata.responsePolicy != null
          ? metadata.responsePolicy.toUpperCase()
          : null;

      MetadataKey key = new MetadataKey(javaFlags, requestPolicy, responsePolicy);
      result.computeIfAbsent(key, k -> new ArrayList<>()).add(commandUpper);
    }

    return result;
  }

  private String generateRegistryClass(Map<MetadataKey, List<String>> metadataCombinations) {
    StringBuilder sb = new StringBuilder();

    // Package and imports
    sb.append("package redis.clients.jedis;\n\n");
    sb.append("import java.util.EnumSet;\n");
    sb.append("import static redis.clients.jedis.StaticCommandFlagsRegistry.EMPTY_FLAGS;\n");
    sb.append("import static redis.clients.jedis.CommandFlagsRegistry.CommandFlag;\n");
    sb.append("import static redis.clients.jedis.CommandFlagsRegistry.RequestPolicy;\n");
    sb.append("import static redis.clients.jedis.CommandFlagsRegistry.ResponsePolicy;\n");

    // Class javadoc
    sb.append("/**\n");
    sb.append(
      " * Static implementation of CommandFlagsRegistry. This class is auto-generated by\n");
    sb.append(" * CommandFlagsRegistryGenerator. DO NOT EDIT MANUALLY.\n");

    // Add server metadata if available
    if (serverMetadata != null) {
      sb.append(" * <p>Generated from Redis Server:\n");
      sb.append(" * <ul>\n");
      sb.append(" * <li>Version: ").append(serverMetadata.version).append("</li>\n");
      sb.append(" * <li>Mode: ").append(serverMetadata.mode).append("</li>\n");
      if (!serverMetadata.modules.isEmpty()) {
        sb.append(" * <li>Loaded Modules: ").append(String.join(", ", serverMetadata.modules))
            .append("</li>\n");
      } else {
        sb.append(" * <li>Loaded Modules: none</li>\n");
      }
      sb.append(" * <li>Generated at: ").append(serverMetadata.generatedAt).append("</li>\n");
      sb.append(" * </ul>\n");
    }

    sb.append(" */\n");
    sb.append("final class StaticCommandFlagsRegistryInitializer {\n\n");

    // Static initializer block
    sb.append("  static void initialize(StaticCommandFlagsRegistry.Builder builder) {\n");

    // Organize commands into parent commands and simple commands
    Map<String, Map<String, MetadataKey>> parentCommands = new LinkedHashMap<>();
    Map<String, MetadataKey> simpleCommands = new LinkedHashMap<>();

    // Categorize commands
    for (Map.Entry<MetadataKey, List<String>> entry : metadataCombinations.entrySet()) {
      MetadataKey metadataKey = entry.getKey();
      for (String command : entry.getValue()) {
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex > 0) {
          // This is a compound command (e.g., "FUNCTION LOAD")
          String parent = command.substring(0, spaceIndex);
          String subcommand = command.substring(spaceIndex + 1);

          if (KNOWN_PARENT_COMMANDS.contains(parent)) {
            parentCommands.computeIfAbsent(parent, k -> new LinkedHashMap<>()).put(subcommand,
              metadataKey);
          } else {
            // Unknown parent command with subcommands - fail with helpful error message
            throw new IllegalStateException(String.format(
              "Unknown command with subcommands encountered: '%s' (parent: '%s', subcommand: '%s'). "
                  + "Command names cannot contain spaces unless the parent command is registered. "
                  + "To fix this, add '%s' to the 'KNOWN_PARENT_COMMANDS' set in CommandFlagsRegistryGenerator.java.",
              command, parent, subcommand, parent));
          }
        } else {
          // Simple command without subcommands
          simpleCommands.put(command, metadataKey);
        }
      }
    }

    // Generate parent command registries
    for (String parent : KNOWN_PARENT_COMMANDS) {
      // Use parent command's actual metadata if available, otherwise use EMPTY_FLAGS
      MetadataKey parentMetadata = simpleCommands.remove(parent);
      if (parentMetadata != null) {
        sb.append(generateRegisterCall(parent, null, parentMetadata));
      } else {
        sb.append(String.format("    builder.register(\"%s\", EMPTY_FLAGS);\n", parent));
      }

      Map<String, MetadataKey> subcommands = parentCommands.get(parent);
      if (subcommands != null && !subcommands.isEmpty()) {
        sb.append(String.format("    // %s subcommands\n", parent));
        // Add subcommands
        List<String> sortedSubcommands = new ArrayList<>(subcommands.keySet());
        Collections.sort(sortedSubcommands);

        for (String subcommand : sortedSubcommands) {
          MetadataKey metadataKey = subcommands.get(subcommand);
          sb.append(generateRegisterCall(parent, subcommand, metadataKey));
        }
      }
    }

    // Generate simple commands grouped by metadata
    Map<MetadataKey, List<String>> simpleCommandsByMetadata = new LinkedHashMap<>();
    for (Map.Entry<String, MetadataKey> entry : simpleCommands.entrySet()) {
      simpleCommandsByMetadata.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
          .add(entry.getKey());
    }

    // Sort by flag count, then alphabetically
    List<Map.Entry<MetadataKey, List<String>>> sortedEntries = simpleCommandsByMetadata.entrySet()
        .stream()
        .sorted(
          Comparator.comparing((Map.Entry<MetadataKey, List<String>> e) -> e.getKey().flags.size())
              .thenComparing(e -> e.getKey().toString()))
        .collect(Collectors.toList());

    for (Map.Entry<MetadataKey, List<String>> entry : sortedEntries) {
      MetadataKey metadataKey = entry.getKey();
      List<String> commands = entry.getValue();
      Collections.sort(commands);

      // Add comment describing the metadata
      sb.append(String.format("    // %d command(s) with: %s\n", commands.size(),
        metadataKey.toDescription()));

      // Add registry entries
      for (String command : commands) {
        sb.append(generateRegisterCall(command, null, metadataKey));
      }
      sb.append("\n");
    }

    // Close initializer block
    sb.append("  }\n\n");

    // Close class
    sb.append("}\n");

    return sb.toString();
  }

  /**
   * Generate a builder.register() call for a command with its metadata.
   */
  private String generateRegisterCall(String command, String subcommand, MetadataKey metadataKey) {
    String enumSetExpr = createEnumSetExpression(metadataKey.flags);
    String requestPolicyExpr = metadataKey.requestPolicy != null
        ? "RequestPolicy." + metadataKey.requestPolicy
        : "null";
    String responsePolicyExpr = metadataKey.responsePolicy != null
        ? "ResponsePolicy." + metadataKey.responsePolicy
        : "null";

    // Check if we need to use the extended register method (with policies)
    boolean hasPolicies = metadataKey.requestPolicy != null || metadataKey.responsePolicy != null;

    if (subcommand != null) {
      // Subcommand registration
      if (hasPolicies) {
        return String.format("    builder.register(\"%s\", \"%s\", %s, %s, %s);\n", command,
          subcommand, enumSetExpr, requestPolicyExpr, responsePolicyExpr);
      } else {
        return String.format("    builder.register(\"%s\", \"%s\", %s);\n", command, subcommand,
          enumSetExpr);
      }
    } else {
      // Simple command registration
      if (hasPolicies) {
        return String.format("    builder.register(\"%s\", %s, %s, %s);\n", command, enumSetExpr,
          requestPolicyExpr, responsePolicyExpr);
      } else {
        return String.format("    builder.register(\"%s\", %s);\n", command, enumSetExpr);
      }
    }
  }

  private String createEnumSetExpression(List<String> flags) {
    if (flags.isEmpty()) {
      return "EMPTY_FLAGS";
    } else if (flags.size() == 1) {
      return "EnumSet.of(CommandFlag." + flags.get(0) + ")";
    } else {
      String flagsList = flags.stream().map(f -> "CommandFlag." + f)
          .collect(Collectors.joining(", "));
      return "EnumSet.of(" + flagsList + ")";
    }
  }

  private void writeJavaFile(String classContent) throws IOException {
    Path javaPath = Paths.get(JAVA_FILE);

    // JDK 8 compatible: write string as bytes
    Files.write(javaPath, classContent.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Holds command metadata extracted from Redis (flags, request_policy, response_policy). Used for
   * JSON serialization/deserialization.
   */
  private static class CommandMetadata {
    final List<String> flags;
    final String requestPolicy;
    final String responsePolicy;

    CommandMetadata(List<String> flags, String requestPolicy, String responsePolicy) {
      this.flags = flags != null ? flags : new ArrayList<>();
      this.requestPolicy = requestPolicy;
      this.responsePolicy = responsePolicy;
    }
  }

  /**
   * Represents a unique combination of flags, request policy, and response policy for grouping
   * commands.
   */
  private static class MetadataKey {
    final List<String> flags;
    final String requestPolicy;
    final String responsePolicy;
    final int hashCode;

    MetadataKey(List<String> flags, String requestPolicy, String responsePolicy) {
      this.flags = new ArrayList<>(flags);
      this.requestPolicy = requestPolicy;
      this.responsePolicy = responsePolicy;
      this.hashCode = Objects.hash(this.flags, requestPolicy, responsePolicy);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MetadataKey)) return false;
      MetadataKey that = (MetadataKey) o;
      return flags.equals(that.flags) && Objects.equals(requestPolicy, that.requestPolicy)
          && Objects.equals(responsePolicy, that.responsePolicy);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      return String.format("flags=%s, request=%s, response=%s", flags, requestPolicy,
        responsePolicy);
    }

    /**
     * Generate a human-readable description for comments.
     */
    String toDescription() {
      StringBuilder sb = new StringBuilder();
      if (flags.isEmpty()) {
        sb.append("no flags");
      } else {
        sb.append(flags.stream().map(String::toLowerCase).collect(Collectors.joining(", ")));
      }
      if (requestPolicy != null) {
        sb.append("; request_policy=").append(requestPolicy.toLowerCase());
      }
      if (responsePolicy != null) {
        sb.append("; response_policy=").append(responsePolicy.toLowerCase());
      }
      return sb.toString();
    }
  }

  /**
   * Holds metadata about the Redis server used for generation
   */
  private static class ServerMetadata {
    final String version;
    final String mode;
    final List<String> modules;
    final String generatedAt;

    ServerMetadata(String version, String mode, List<String> modules) {
      this.version = version;
      this.mode = mode;
      this.modules = modules;
      this.generatedAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
          .format(new java.util.Date());
    }
  }
}
