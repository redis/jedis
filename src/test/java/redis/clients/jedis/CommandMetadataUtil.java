package redis.clients.jedis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import redis.clients.jedis.MetadataResolver.CommandMetadata;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Maintenance tool (not a test) that regenerates the shared command metadata from live Redis
 * {@code COMMAND INFO} output.
 * <p>
 * It performs two steps:
 * <ol>
 * <li>Fetches {@code COMMAND INFO} for every command (container subcommands included, keyed
 * {@code PARENT|CHILD}) and writes the normalized metadata to {@code CommandMetadata.json} in the
 * repository root. The file carries metadata only; cacheability is not part of it.</li>
 * <li>Regenerates the metadata table of {@code MetadataResolver.java}: one entry per command,
 * container subcommands included. Only the region between the {@code GENERATED-METADATA-BEGIN} /
 * {@code GENERATED-METADATA-END} markers is replaced; everything else in the file is
 * preserved.</li>
 * </ol>
 * Consumers such as cacheability resolution and the command-flags registry build on the generated
 * metadata; this tool carries no consumer logic itself. Mismatches between the Jedis command set
 * and the server metadata are reported as warnings: {@link ProtocolCommand} constants without
 * server metadata, and server commands without a constant in Jedis.
 * <p>
 * Usage (run from the repository root, paths are resolved relative to the working directory):
 *
 * <pre>
 * mvn -q test-compile exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=redis.clients.jedis.CommandMetadataUtil \
 *     -Dexec.args="-remote:localhost:6379 -user:default -auth:secret"
 * </pre>
 *
 * All arguments are optional: {@code -remote:host:port} defaults to {@code localhost:6379}, and
 * {@code -auth:password} / {@code -user:username} authenticate the connection when given. When the
 * server is unreachable, the tool falls back to the existing {@code CommandMetadata.json}: the file
 * is left untouched (it is the source) and only {@code MetadataResolver.java} is regenerated from
 * it.
 */
public class CommandMetadataUtil {

  private static final Path COMMAND_INFO_JSON = Paths.get("CommandMetadata.json");
  private static final Path METADATA_RESOLVER_JAVA = Paths.get("src", "main", "java", "redis",
    "clients", "jedis", "MetadataResolver.java");

  private static final String METADATA_BEGIN = "// GENERATED-METADATA-BEGIN";
  private static final String METADATA_END = "// GENERATED-METADATA-END";

  /** Entries per generated init method, keeping each method well under the JVM bytecode limit. */
  private static final int CHUNK_SIZE = 250;

  private static final Set<String> NESTED_SPEC_KEYS = new HashSet<>(
      Arrays.asList("begin_search", "find_keys", "spec"));

  /** Normalized command metadata. */
  private static final class CommandMeta {
    String name;
    TreeSet<String> flags;
    TreeSet<String> tips;
    long firstKey;
    long lastKey;
    long step;
    List<Object> keySpecs;
    boolean hasKeyNameSpec;
    final List<CommandMeta> subcommands = new ArrayList<>(0);
  }

  public static void main(String[] args) throws IOException {
    Arguments arguments = parseArguments(args);
    requireFile(METADATA_RESOLVER_JAVA);
    String generatedAt = ZonedDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

    TreeMap<String, CommandMeta> metas;
    String source;
    try (Jedis jedis = new Jedis(arguments.target, arguments.clientConfig())) {
      System.out.println("Step 1: Fetching COMMAND INFO from " + arguments.target + "...");
      metas = fetchCommandMetadata(jedis);
      ServerIdentity server = serverIdentity(jedis);
      System.out.println("✓ Retrieved " + metas.size() + " commands from Redis " + server.version
          + " (" + server.mode + "; modules: " + String.join(", ", server.modules) + ")");

      System.out.println("Step 2: Writing " + COMMAND_INFO_JSON + "...");
      writeCommandMetadataJson(metas, server, arguments.target, generatedAt);
      source = "COMMAND INFO from Redis " + server.version + " (" + server.mode + "; modules: "
          + String.join(", ", server.modules) + ")";
    } catch (JedisConnectionException jce) {
      System.out.println("✗ Cannot connect to " + arguments.target + " (" + jce.getMessage()
          + "); falling back to " + COMMAND_INFO_JSON);
      metas = readFromCommandMetadataJson();
      String version = redisVersionFromJsonFile();
      System.out.println("✓ Loaded " + metas.size() + " commands from " + COMMAND_INFO_JSON
          + " (Redis " + version + "); the file is left untouched");
      source = COMMAND_INFO_JSON + " (Redis " + version + ")";
    }

    System.out.println("Step 3: Regenerating " + METADATA_RESOLVER_JAVA + "...");
    updateMetadataResolver(metas, source + ". Generated at " + generatedAt);
  }

  /** Parsed command-line arguments. */
  private static final class Arguments {
    HostAndPort target = new HostAndPort("localhost", 6379);
    String user;
    String password;

    JedisClientConfig clientConfig() {
      DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
      if (password != null) {
        builder.password(password);
      }
      if (user != null) {
        builder.user(user);
      }
      return builder.build();
    }
  }

  private static Arguments parseArguments(String[] args) {

    Arguments arguments = new Arguments();
    for (String arg : args) {
      System.out.println("Parsing argument: " + arg);
      if (arg.startsWith("-remote:")) {
        String rest = arg.substring("-remote:".length());
        int sep = rest.lastIndexOf(':');
        if (sep <= 0 || sep == rest.length() - 1) {
          usage(arg);
        }
        try {
          arguments.target = new HostAndPort(rest.substring(0, sep),
              Integer.parseInt(rest.substring(sep + 1)));
        } catch (NumberFormatException nfe) {
          usage(arg);
        }
      } else if (arg.startsWith("-auth:")) {
        arguments.password = arg.substring("-auth:".length());
      } else if (arg.startsWith("-user:")) {
        arguments.user = arg.substring("-user:".length());
      } else {
        usage(arg);
      }
    }
    return arguments;
  }

  private static void usage(String offending) {
    System.err.println("Unrecognized argument: " + offending);
    System.err.println(
      "Usage: CommandMetadataUtil [-remote:host:port] [-auth:password] [-user:username]");
    System.exit(2);
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("Cannot find " + path
          + "; run this tool from the repository root so relative paths resolve.");
    }
  }

  // ---------------------------------------------------------- metadata fetch

  private static TreeMap<String, CommandMeta> fetchCommandMetadata(Jedis jedis) {
    Object reply = jedis.sendCommand(Command.COMMAND, "INFO");
    TreeMap<String, CommandMeta> out = new TreeMap<>();
    for (Object entry : (List<?>) reply) {
      if (entry instanceof List) {
        collect((List<?>) entry, null, out);
      }
    }
    return out;
  }

  private static void collect(List<?> entry, CommandMeta parent, TreeMap<String, CommandMeta> out) {
    CommandMeta m = new CommandMeta();
    String name = str(entry.get(0)).toUpperCase();
    // container subcommands report either CHILD or PARENT|CHILD depending on server version
    m.name = (parent != null && name.indexOf('|') < 0) ? parent.name + '|' + name : name;
    if (parent != null) {
      parent.subcommands.add(m);
    }
    m.flags = lowerSet(at(entry, 2));
    m.firstKey = num(at(entry, 3));
    m.lastKey = num(at(entry, 4));
    m.step = num(at(entry, 5));
    m.tips = lowerSet(at(entry, 7));
    m.keySpecs = normalizeKeySpecs(at(entry, 8));
    m.hasKeyNameSpec = hasKeyNameSpec(m.keySpecs);
    out.put(m.name, m);
    Object subs = at(entry, 9);
    if (subs instanceof List) {
      for (Object sub : (List<?>) subs) {
        if (sub instanceof List) {
          collect((List<?>) sub, m, out);
        }
      }
    }
  }

  /** Server identity captured for provenance: version, mode, and loaded modules. */
  private static final class ServerIdentity {
    final String version;
    final String mode;
    final List<String> modules;

    ServerIdentity(String version, String mode, List<String> modules) {
      this.version = version;
      this.mode = mode;
      this.modules = modules;
    }
  }

  private static ServerIdentity serverIdentity(Jedis jedis) {
    String version = "unknown";
    String mode = "unknown";
    for (String line : jedis.info("server").split("\r?\n")) {
      if (line.startsWith("redis_version:")) {
        version = line.substring("redis_version:".length()).trim();
      } else if (line.startsWith("redis_mode:")) {
        mode = line.substring("redis_mode:".length()).trim();
      }
    }
    List<String> modules = new ArrayList<>();
    try {
      for (Module module : jedis.moduleList()) {
        modules.add(module.getName());
      }
    } catch (Exception e) {
      System.out.println("  Note: could not retrieve module list: " + e.getMessage());
    }
    return new ServerIdentity(version, mode, modules);
  }

  /** Fallback source: the checked-in metadata file, via the same reader the resolver uses. */
  private static TreeMap<String, CommandMeta> readFromCommandMetadataJson() {
    TreeMap<String, CommandMeta> out = new TreeMap<>();
    Map<String, CommandMetadata> parsed = MetadataReader.read(COMMAND_INFO_JSON);
    for (CommandMetadata metadata : parsed.values()) {
      fromMetadata(metadata, out);
    }
    for (CommandMetadata metadata : parsed.values()) {
      CommandMeta m = out.get(metadata.getName());
      for (CommandMetadata subcommand : metadata.getSubcommands()) {
        m.subcommands.add(out.get(subcommand.getName()));
      }
    }
    return out;
  }

  private static void fromMetadata(CommandMetadata metadata, TreeMap<String, CommandMeta> out) {
    if (out.containsKey(metadata.getName())) {
      return;
    }
    CommandMeta m = new CommandMeta();
    m.name = metadata.getName();
    m.flags = new TreeSet<>(metadata.getFlags());
    m.tips = new TreeSet<>(metadata.getTips());
    m.firstKey = metadata.getFirstKey();
    m.lastKey = metadata.getLastKey();
    m.step = metadata.getStep();
    m.keySpecs = new ArrayList<>();
    m.hasKeyNameSpec = metadata.hasKeyNameSpec();
    out.put(m.name, m);
  }

  private static String redisVersionFromJsonFile() throws IOException {
    String json = new String(Files.readAllBytes(COMMAND_INFO_JSON), StandardCharsets.UTF_8);
    String marker = "\"redisVersion\": \"";
    int begin = json.indexOf(marker);
    if (begin < 0) {
      return "unknown";
    }
    int end = json.indexOf('"', begin + marker.length());
    return json.substring(begin + marker.length(), end);
  }

  // ------------------------------------------------- RESP reply normalization

  private static List<Object> normalizeKeySpecs(Object specs) {
    List<Object> out = new ArrayList<>();
    if (specs instanceof List) {
      for (Object spec : (List<?>) specs) {
        out.add(normalizeKeySpec(spec));
      }
    }
    return out;
  }

  private static Object normalizeKeySpec(Object specObj) {
    Map<String, Object> raw = pairsToMap(specObj);
    if (raw == null) {
      return scalarTree(specObj); // unexpected shape: keep verbatim rather than fail
    }
    LinkedHashMap<String, Object> out = new LinkedHashMap<>();
    List<String> flags = new ArrayList<>();
    Object rawFlags = raw.get("flags");
    if (rawFlags instanceof List) {
      for (Object f : (List<?>) rawFlags) {
        flags.add(String.valueOf(f).toLowerCase());
      }
    }
    out.put("flags", flags);
    if (raw.containsKey("notes")) {
      out.put("notes", raw.get("notes"));
    }
    if (raw.containsKey("begin_search")) {
      out.put("beginSearch", raw.get("begin_search"));
    }
    if (raw.containsKey("find_keys")) {
      out.put("findKeys", raw.get("find_keys"));
    }
    return out;
  }

  /** RESP2 key specs are flat alternating key/value lists; convert to a map, recursing on specs. */
  private static Map<String, Object> pairsToMap(Object o) {
    if (!(o instanceof List)) {
      return null;
    }
    List<?> list = (List<?>) o;
    if (list.size() % 2 != 0) {
      return null;
    }
    LinkedHashMap<String, Object> out = new LinkedHashMap<>();
    for (int i = 0; i < list.size(); i += 2) {
      Object k = list.get(i);
      if (!(k instanceof byte[]) && !(k instanceof String)) {
        return null;
      }
      String key = str(k);
      Object value = list.get(i + 1);
      if (NESTED_SPEC_KEYS.contains(key) && value instanceof List) {
        Map<String, Object> nested = pairsToMap(value);
        out.put(key, nested != null ? nested : scalarTree(value));
      } else {
        out.put(key, scalarTree(value));
      }
    }
    return out;
  }

  private static Object scalarTree(Object v) {
    if (v instanceof byte[]) {
      return SafeEncoder.encode((byte[]) v);
    }
    if (v instanceof List) {
      List<Object> out = new ArrayList<>();
      for (Object e : (List<?>) v) {
        out.add(scalarTree(e));
      }
      return out;
    }
    return v;
  }

  private static Object at(List<?> list, int index) {
    return index < list.size() ? list.get(index) : null;
  }

  private static String str(Object o) {
    return o instanceof byte[] ? SafeEncoder.encode((byte[]) o) : String.valueOf(o);
  }

  private static long num(Object o) {
    return o instanceof Number ? ((Number) o).longValue() : 0L;
  }

  private static TreeSet<String> lowerSet(Object o) {
    TreeSet<String> out = new TreeSet<>();
    if (o instanceof List) {
      for (Object v : (List<?>) o) {
        out.add(str(v).toLowerCase());
      }
    }
    return out;
  }

  // ---------------------------------------------------------- CommandMetadata.json

  private static void writeCommandMetadataJson(TreeMap<String, CommandMeta> metas,
      ServerIdentity server, HostAndPort target, String generatedAt) throws IOException {
    int topLevel = 0;
    TreeMap<String, Object> commands = new TreeMap<>();
    for (CommandMeta m : metas.values()) {
      if (m.name.indexOf('|') < 0) {
        topLevel++;
        commands.put(m.name, jsonEntry(m));
      }
    }

    LinkedHashMap<String, Object> counts = new LinkedHashMap<>();
    counts.put("commands", (long) topLevel);
    counts.put("subcommands", (long) (metas.size() - topLevel));
    counts.put("total", (long) metas.size());

    LinkedHashMap<String, Object> doc = new LinkedHashMap<>();
    doc.put("redisVersion", server.version);
    doc.put("redisMode", server.mode);
    doc.put("modules", server.modules);
    doc.put("generatedAt", generatedAt);
    doc.put("source", "COMMAND INFO (" + target.getHost() + ":" + target.getPort() + ")");
    doc.put("counts", counts);
    doc.put("commands", commands);

    StringBuilder sb = new StringBuilder(1 << 20);
    appendJson(doc, 0, sb);
    sb.append('\n');
    Files.write(COMMAND_INFO_JSON, sb.toString().getBytes(StandardCharsets.UTF_8));
    System.out.println("Wrote " + COMMAND_INFO_JSON + " (" + topLevel + " commands, "
        + (metas.size() - topLevel) + " subcommands)");
  }

  /** One command as a JSON object; subcommands nested in hierarchy, as the server reports them. */
  private static LinkedHashMap<String, Object> jsonEntry(CommandMeta m) {
    LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
    entry.put("name", m.name);
    entry.put("flags", new ArrayList<Object>(m.flags));
    entry.put("tips", new ArrayList<Object>(m.tips));
    entry.put("firstKey", m.firstKey);
    entry.put("lastKey", m.lastKey);
    entry.put("step", m.step);
    entry.put("keySpecs", m.keySpecs);
    if (!m.subcommands.isEmpty()) {
      TreeMap<String, Object> subcommands = new TreeMap<>();
      for (CommandMeta subcommand : m.subcommands) {
        subcommands.put(subcommand.name, jsonEntry(subcommand));
      }
      entry.put("subcommands", subcommands);
    }
    return entry;
  }

  // hand-rolled writer: keeps deterministic key order and stable formatting for reviewable
  // diffs, which org.json (unordered maps) cannot guarantee
  private static void appendJson(Object value, int indent, StringBuilder out) {
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      if (map.isEmpty()) {
        out.append("{}");
        return;
      }
      out.append("{\n");
      int i = 0;
      for (Map.Entry<?, ?> e : map.entrySet()) {
        pad(indent + 2, out);
        appendJsonString(String.valueOf(e.getKey()), out);
        out.append(": ");
        appendJson(e.getValue(), indent + 2, out);
        if (++i < map.size()) {
          out.append(',');
        }
        out.append('\n');
      }
      pad(indent, out);
      out.append('}');
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      if (list.isEmpty()) {
        out.append("[]");
        return;
      }
      out.append("[\n");
      for (int i = 0; i < list.size(); i++) {
        pad(indent + 2, out);
        appendJson(list.get(i), indent + 2, out);
        if (i < list.size() - 1) {
          out.append(',');
        }
        out.append('\n');
      }
      pad(indent, out);
      out.append(']');
    } else if (value instanceof String) {
      appendJsonString((String) value, out);
    } else if (value instanceof Boolean || value instanceof Number) {
      out.append(value);
    } else if (value == null) {
      out.append("null");
    } else {
      appendJsonString(String.valueOf(value), out);
    }
  }

  private static void appendJsonString(String s, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"':
          out.append("\\\"");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\b':
          out.append("\\b");
          break;
        case '\f':
          out.append("\\f");
          break;
        default:
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
      }
    }
    out.append('"');
  }

  private static void pad(int n, StringBuilder out) {
    for (int i = 0; i < n; i++) {
      out.append(' ');
    }
  }

  // ------------------------------------------------------- MetadataResolver.java

  private static void updateMetadataResolver(TreeMap<String, CommandMeta> metas, String source)
      throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("  // Source: " + source + ".");

    int chunks = 0;
    int inChunk = 0;
    for (CommandMeta m : metas.values()) {
      if (m.name.indexOf('|') >= 0) {
        continue; // emitted with its parent, keeping the hierarchy together
      }
      // a container and its subcommands stay in the same chunk
      if (inChunk == 0 || inChunk + 1 + m.subcommands.size() > CHUNK_SIZE) {
        if (inChunk > 0) {
          lines.add("  }");
          lines.add("");
          chunks++;
        }
        lines.add("  private static void init" + chunks + "(Map<String, CommandMetadata> t) {");
        inChunk = 0;
      }
      if (m.subcommands.isEmpty()) {
        lines.add("    " + addCall(m) + ";");
      } else {
        String variable = variableName(m.name);
        lines.add("    CommandMetadata " + variable + " = " + addCall(m) + ";");
        List<CommandMeta> ordered = new ArrayList<>(m.subcommands);
        ordered.sort((a, b) -> a.name.compareTo(b.name)); // deterministic, reproducible output
        for (CommandMeta subcommand : ordered) {
          lines.add("    " + variable + ".addSubcommand(" + addCall(subcommand) + ");");
        }
      }
      inChunk += 1 + m.subcommands.size();
    }
    if (inChunk > 0) {
      lines.add("  }");
      lines.add("");
      chunks++;
    }
    lines.add("  private static void registerAll(Map<String, CommandMetadata> t) {");
    for (int i = 0; i < chunks; i++) {
      lines.add("    init" + i + "(t);");
    }
    lines.add("  }");

    String content = new String(Files.readAllBytes(METADATA_RESOLVER_JAVA), StandardCharsets.UTF_8);
    content = spliceRegion(content, METADATA_BEGIN, METADATA_END, lines);
    Files.write(METADATA_RESOLVER_JAVA, content.getBytes(StandardCharsets.UTF_8));
    System.out.println("Updated " + METADATA_RESOLVER_JAVA + " (" + metas.size()
        + " metadata entries in " + chunks + " chunks)");

    reportMismatches(metas);
  }

  private static String addCall(CommandMeta m) {
    return "add(t, \"" + m.name + "\", " + setExpression(m.flags) + ", " + setExpression(m.tips)
        + ", " + m.firstKey + ", " + m.lastKey + ", " + m.step + ", " + m.hasKeyNameSpec + ")";
  }

  private static String setExpression(TreeSet<String> values) {
    if (values.isEmpty()) {
      return "Collections.emptySet()";
    }
    StringBuilder sb = new StringBuilder("new HashSet<>(Arrays.asList(");
    boolean first = true;
    for (String value : values) {
      if (!first) {
        sb.append(", ");
      }
      sb.append('"').append(value).append('"');
      first = false;
    }
    return sb.append("))").toString();
  }

  /** Local variable name for a container command, e.g. {@code FT.CONFIG -> ftConfigMetadata}. */
  private static String variableName(String commandName) {
    StringBuilder sb = new StringBuilder();
    if (commandName.startsWith("_")) {
      sb.append('_');
    }
    boolean upper = false;
    for (char c : commandName.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
        upper = false;
      } else {
        upper = sb.length() > (commandName.startsWith("_") ? 1 : 0);
      }
    }
    return sb.append("Metadata").toString();
  }

  private static boolean hasKeyNameSpec(List<Object> keySpecs) {
    for (Object specObj : keySpecs) {
      if (specObj instanceof Map) {
        Object flags = ((Map<?, ?>) specObj).get("flags");
        boolean notKey = flags instanceof List && ((List<?>) flags).contains("not_key");
        if (!notKey) {
          return true;
        }
      }
    }
    return false;
  }

  /** Mismatches between the Jedis command set and the server metadata. */
  private static void reportMismatches(TreeMap<String, CommandMeta> metas) {
    Set<String> jedisWires = new HashSet<>();
    for (ProtocolCommand[] values : MetadataResolver.protocolCommandEnums()) {
      for (ProtocolCommand value : values) {
        Enum<?> constant = (Enum<?>) value;
        String wire = SafeEncoder.encode(value.getRaw()).toUpperCase();
        if (jedisWires.add(wire) && !metas.containsKey(wire)) {
          System.out.println("WARNING: no COMMAND metadata for "
              + constant.getDeclaringClass().getSimpleName() + '.' + constant.name() + " (" + wire
              + "); metadata consumers will treat it as unknown.");
        }
      }
    }
    for (CommandMeta m : metas.values()) {
      if (m.name.indexOf('|') < 0 && !jedisWires.contains(m.name)) {
        System.out.println("WARNING: no ProtocolCommand constant in Jedis for " + m.name + ".");
      }
    }
    // a metadata fix is obsolete once the server reports the fixed tips natively; the fixes
    // themselves live in MetadataResolver, so this is a pure metadata comparison
    for (Map.Entry<String, String[]> fix : MetadataResolver.knownServerMetadataFixes().entrySet()) {
      CommandMeta m = metas.get(fix.getKey());
      if (m == null) {
        System.out.println("WARNING: metadata fix for " + fix.getKey()
            + " targets a command the server no longer reports; remove it from MetadataResolver.");
      } else if (m.tips.containsAll(Arrays.asList(fix.getValue()))) {
        System.out.println(
          "WARNING: metadata fix for " + fix.getKey() + " is obsolete (the server already reports: "
              + String.join(" ", fix.getValue()) + "); remove it from MetadataResolver.");
      }
    }
  }

  /** Replace the lines between two marker lines, keeping the marker lines themselves. */
  private static String spliceRegion(String content, String beginMarker, String endMarker,
      List<String> newLines) {
    int begin = content.indexOf(beginMarker);
    int end = content.indexOf(endMarker);
    if (begin < 0 || end < 0 || end < begin) {
      throw new IllegalStateException("Markers " + beginMarker + " / " + endMarker
          + " not found in " + METADATA_RESOLVER_JAVA + "; restore them before regenerating.");
    }
    int afterBeginLine = content.indexOf('\n', begin) + 1;
    int endLineStart = content.lastIndexOf('\n', end) + 1;
    StringBuilder sb = new StringBuilder(content.length() + 1024);
    sb.append(content, 0, afterBeginLine);
    for (String line : newLines) {
      sb.append(line).append('\n');
    }
    sb.append(content, endLineStart, content.length());
    return sb.toString();
  }
}
