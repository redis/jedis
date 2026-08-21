package redis.clients.jedis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import redis.clients.jedis.MetadataResolver.CommandMetadata;

/**
 * Reads a command-metadata file with the {@code CommandMetadata.json} layout (produced by
 * {@code CommandMetadataUtil}) into the normalized {@link CommandMetadata} shape. Command names,
 * container subcommands included ({@code PARENT|CHILD}), are the map keys.
 */
final class MetadataReader {

  private MetadataReader() {
  }

  /**
   * Fails fast on a missing or malformed file: an explicitly configured metadata source must not be
   * silently ignored.
   */
  static Map<String, CommandMetadata> read(Path path) {
    try {
      String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
      JSONObject commands = new JSONObject(json).getJSONObject("commands");
      Map<String, CommandMetadata> table = new HashMap<>(Math.max(16, commands.length() * 2));
      for (String key : commands.keySet()) {
        parse(key, commands.getJSONObject(key), table);
      }
      return table;
    } catch (IOException | JSONException e) {
      throw new IllegalStateException("Failed to read command metadata from " + path, e);
    }
  }

  /**
   * Parses one command entry into the flat table; container subcommands are nested under their
   * parent as the server reports them, and are attached to the parent's hierarchy as well as
   * registered in the table by their full {@code PARENT|CHILD} name.
   */
  private static CommandMetadata parse(String key, JSONObject command,
      Map<String, CommandMetadata> table) {
    String name = key.toUpperCase();
    CommandMetadata metadata = new CommandMetadata(name, toSet(command, "flags"),
        toSet(command, "tips"), command.getLong("firstKey"), command.getLong("lastKey"),
        command.getLong("step"), hasKeyNameSpec(command.optJSONArray("keySpecs")));
    table.put(name, metadata);
    JSONObject subcommands = command.optJSONObject("subcommands");
    if (subcommands != null) {
      for (String subKey : subcommands.keySet()) {
        metadata.addSubcommand(parse(subKey, subcommands.getJSONObject(subKey), table));
      }
    }
    return metadata;
  }

  private static Set<String> toSet(JSONObject command, String key) {
    JSONArray values = command.optJSONArray(key);
    if (values == null || values.length() == 0) {
      return Collections.emptySet();
    }
    Set<String> set = new HashSet<>(Math.max(4, values.length() * 2));
    for (int i = 0; i < values.length(); i++) {
      set.add(values.getString(i).toLowerCase());
    }
    return set;
  }

  /** Same rule as the generator: a key spec without the {@code not_key} flag names a key. */
  private static boolean hasKeyNameSpec(JSONArray keySpecs) {
    if (keySpecs == null) {
      return false;
    }
    for (int i = 0; i < keySpecs.length(); i++) {
      JSONObject spec = keySpecs.optJSONObject(i);
      if (spec == null) {
        continue;
      }
      JSONArray flags = spec.optJSONArray("flags");
      boolean notKey = false;
      for (int j = 0; flags != null && j < flags.length(); j++) {
        if ("not_key".equals(flags.optString(j))) {
          notKey = true;
          break;
        }
      }
      if (!notKey) {
        return true;
      }
    }
    return false;
  }
}
