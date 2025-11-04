package redis.clients.jedis.codegen;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Module;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
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
  private static final String BACKUP_JSON_FILE = "redis_commands_flags.json";

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
    Map<String, List<String>> commandsFlags;

    // Step 1: Retrieve commands from Redis
    System.out.println("\nStep 1: Connecting to Redis at " + redisHost + ":" + redisPort + "...");
    try {
      commandsFlags = retrieveCommandsFromRedis();
      System.out.println("✓ Retrieved " + commandsFlags.size() + " commands from Redis");

      // Save to backup JSON file
      saveToJsonFile(commandsFlags);
    } catch (JedisConnectionException e) {
      System.err.println("✗ Failed to connect to Redis: " + e.getMessage());
      System.out.println("\nAttempting to use backup JSON file: " + BACKUP_JSON_FILE);
      commandsFlags = readJsonFile();
      System.out.println("✓ Loaded " + commandsFlags.size() + " commands from backup file");
    }

    // Step 2: Process commands and group by flag combinations
    System.out.println("\nStep 2: Processing commands and grouping by flags...");
    Map<FlagSet, List<String>> flagCombinations = groupByFlags(commandsFlags);
    System.out.println("✓ Found " + flagCombinations.size() + " unique flag combinations");

    // Step 3: Generate StaticCommandFlagsRegistry class
    System.out.println("\nStep 3: Generating StaticCommandFlagsRegistry class...");
    String classContent = generateRegistryClass(flagCombinations);
    System.out.println("✓ Generated " + classContent.split("\n").length + " lines of code");

    // Step 4: Write StaticCommandFlagsRegistry.java
    System.out.println("\nStep 4: Writing " + JAVA_FILE + "...");
    writeJavaFile(classContent);
    System.out.println("✓ Successfully created StaticCommandFlagsRegistry.java");
  }

  private Map<String, List<String>> retrieveCommandsFromRedis() {
    Map<String, List<String>> result = new LinkedHashMap<>();

    try (Jedis jedis = new Jedis(redisHost, redisPort)) {
      jedis.connect();

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
      Map<String, redis.clients.jedis.resps.CommandInfo> commands = jedis.command();

      for (Map.Entry<String, redis.clients.jedis.resps.CommandInfo> entry : commands.entrySet()) {
        redis.clients.jedis.resps.CommandInfo cmdInfo = entry.getValue();
        String commandName = normalizeCommandName(cmdInfo.getName());

        // Get flags
        List<String> flags = new ArrayList<>();
        if (cmdInfo.getFlags() != null) {
          for (String flag : cmdInfo.getFlags()) {
            flags.add(flag.toLowerCase());
          }
        }

        // Check for subcommands
        Map<String, redis.clients.jedis.resps.CommandInfo> subcommands = cmdInfo.getSubcommands();

        if (subcommands != null && !subcommands.isEmpty()) {
          // This command has subcommands - process them instead of the parent
          for (Map.Entry<String, redis.clients.jedis.resps.CommandInfo> subEntry : subcommands
              .entrySet()) {
            redis.clients.jedis.resps.CommandInfo subCmdInfo = subEntry.getValue();
            String subCommandName = normalizeCommandName(subCmdInfo.getName());

            // Filter out unwanted commands
            if (shouldExcludeCommand(subCommandName)) {
              continue;
            }

            // Get subcommand flags
            List<String> subFlags = new ArrayList<>();
            if (subCmdInfo.getFlags() != null) {
              for (String flag : subCmdInfo.getFlags()) {
                subFlags.add(flag.toLowerCase());
              }
            }

            result.put(subCommandName, subFlags);
          }
        } else {
          // Regular command without subcommands
          // Filter out unwanted commands
          if (!shouldExcludeCommand(commandName)) {
            result.put(commandName, flags);
          }
        }
      }

    }
    // Ignore close errors

    return result;
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
    return commandName.startsWith("FT.DEBUG ") || commandName.startsWith("_FT.DEBUG ");
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

  private void saveToJsonFile(Map<String, List<String>> commandsFlags) throws IOException {
    Gson gson = new Gson();
    String json = gson.toJson(commandsFlags);

    Path jsonPath = Paths.get(BACKUP_JSON_FILE);
    Files.write(jsonPath, json.getBytes(StandardCharsets.UTF_8));
    System.out.println("✓ Saved backup to " + BACKUP_JSON_FILE);
  }

  private Map<String, List<String>> readJsonFile() throws IOException {
    Path jsonPath = Paths.get(BACKUP_JSON_FILE);
    if (!Files.exists(jsonPath)) {
      throw new IOException("Backup file not found: " + BACKUP_JSON_FILE);
    }

    // JDK 8 compatible: read file as bytes and convert to string
    byte[] bytes = Files.readAllBytes(jsonPath);
    String jsonContent = new String(bytes, StandardCharsets.UTF_8);

    Gson gson = new Gson();

    // Parse JSON manually to preserve order
    @SuppressWarnings("unchecked")
    Map<String, List<String>> parsed = gson.fromJson(jsonContent, Map.class);

    return new LinkedHashMap<>(parsed);
  }

  private Map<FlagSet, List<String>> groupByFlags(Map<String, List<String>> commandsFlags) {
    Map<FlagSet, List<String>> result = new LinkedHashMap<>();

    for (Map.Entry<String, List<String>> entry : commandsFlags.entrySet()) {
      String command = entry.getKey();
      List<String> jsonFlags = entry.getValue();

      // Convert JSON flags to Java enum names and sort
      List<String> javaFlags = jsonFlags.stream().map(f -> FLAG_MAPPING.get(f.toLowerCase()))
          .filter(Objects::nonNull).sorted().collect(Collectors.toList());

      FlagSet flagSet = new FlagSet(javaFlags);
      result.computeIfAbsent(flagSet, k -> new ArrayList<>()).add(command.toUpperCase());
    }

    return result;
  }

  private String generateRegistryClass(Map<FlagSet, List<String>> flagCombinations) {
    StringBuilder sb = new StringBuilder();

    // Package and imports
    sb.append("package redis.clients.jedis;\n\n");
    sb.append("import java.util.EnumSet;\n");
    sb.append("import static redis.clients.jedis.StaticCommandFlagsRegistry.EMPTY_FLAGS;\n");
    sb.append("import static redis.clients.jedis.CommandFlagsRegistry.CommandFlag;\n");


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
    Map<String, Map<String, FlagSet>> parentCommands = new LinkedHashMap<>();
    Map<String, FlagSet> simpleCommands = new LinkedHashMap<>();

    // Known parent commands
    Set<String> knownParents = new HashSet<>(
        Arrays.asList("ACL", "CLIENT", "CLUSTER", "COMMAND", "CONFIG", "FUNCTION", "LATENCY",
          "MEMORY", "MODULE", "OBJECT", "PUBSUB", "SCRIPT", "SLOWLOG", "XGROUP", "XINFO"));

    // Categorize commands
    for (Map.Entry<FlagSet, List<String>> entry : flagCombinations.entrySet()) {
      FlagSet flagSet = entry.getKey();
      for (String command : entry.getValue()) {
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex > 0) {
          // This is a compound command (e.g., "FUNCTION LOAD")
          String parent = command.substring(0, spaceIndex);
          String subcommand = command.substring(spaceIndex + 1);

          if (knownParents.contains(parent)) {
            parentCommands.computeIfAbsent(parent, k -> new LinkedHashMap<>()).put(subcommand,
              flagSet);
          } else {
            // Not a known parent, treat as simple command
            simpleCommands.put(command, flagSet);
          }
        } else {
          // Simple command without subcommands
          simpleCommands.put(command, flagSet);
        }
      }
    }

    // Generate parent command registries
    for (String parent : knownParents) {
      sb.append(String.format("builder.register(\"%s\", EMPTY_FLAGS);", parent));

      Map<String, FlagSet> subcommands = parentCommands.get(parent);
      if (subcommands != null && !subcommands.isEmpty()) {
        sb.append(String.format("    // %s parent command with subcommands\n", parent));
        // Add subcommands
        List<String> sortedSubcommands = new ArrayList<>(subcommands.keySet());
        Collections.sort(sortedSubcommands);

        for (String subcommand : sortedSubcommands) {
          FlagSet flagSet = subcommands.get(subcommand);
          String enumSetExpr = createEnumSetExpression(flagSet.flags);
          sb.append(String.format("builder.register(\"%s\", \"%s\", %s);\n", parent, subcommand, enumSetExpr));
        }
      }
    }

    // Generate simple commands grouped by flags
    Map<FlagSet, List<String>> simpleCommandsByFlags = new LinkedHashMap<>();
    for (Map.Entry<String, FlagSet> entry : simpleCommands.entrySet()) {
      simpleCommandsByFlags.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
          .add(entry.getKey());
    }

    // Sort by flag count, then alphabetically
    List<Map.Entry<FlagSet, List<String>>> sortedEntries = simpleCommandsByFlags.entrySet().stream()
        .sorted(
          Comparator.comparing((Map.Entry<FlagSet, List<String>> e) -> e.getKey().flags.size())
              .thenComparing(e -> e.getKey().toString()))
        .collect(Collectors.toList());

    for (Map.Entry<FlagSet, List<String>> entry : sortedEntries) {
      FlagSet flagSet = entry.getKey();
      List<String> commands = entry.getValue();
      Collections.sort(commands);

      // Add comment
      String flagDesc = flagSet.flags.isEmpty() ? "no flags"
          : flagSet.flags.stream().map(String::toLowerCase).collect(Collectors.joining(", "));
      sb.append(String.format("    // %d command(s) with: %s\n", commands.size(), flagDesc));

      // Generate EnumSet expression
      String enumSetExpr = createEnumSetExpression(flagSet.flags);

      // Add registry entries using SafeEncoder.encode()
      for (String command : commands) {
        sb.append(String.format("builder.register(\"%s\", %s);\n", command, enumSetExpr));
      }
      sb.append("\n");
    }

    // Close initializer block
    sb.append("  }\n\n");

    // Close class
    sb.append("}\n");

    return sb.toString();
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
   * Represents a set of flags for grouping commands
   */
  private static class FlagSet {
    final List<String> flags;
    final int hashCode;

    FlagSet(List<String> flags) {
      this.flags = new ArrayList<>(flags);
      this.hashCode = this.flags.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof FlagSet)) return false;
      FlagSet flagSet = (FlagSet) o;
      return flags.equals(flagSet.flags);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      return flags.toString();
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
