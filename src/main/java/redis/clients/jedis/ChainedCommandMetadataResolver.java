package redis.clients.jedis;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

final class ChainedCommandMetadataResolver implements CommandMetadataResolver {

  private final CommandMetadataResolver primary;
  private final CommandMetadataResolver fallback;

  ChainedCommandMetadataResolver(CommandMetadataResolver primary,
      CommandMetadataResolver fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public CommandMetadata resolve(String commandName) {
    CommandMetadata metadata = primary.resolve(commandName);
    if (metadata != null || ownedByPrimary(commandName)) {
      return metadata;
    }
    return fallback.resolve(commandName);
  }

  @Override
  public Collection<CommandMetadata> allCommandMetadata() {
    Map<String, CommandMetadata> merged = new LinkedHashMap<>();
    for (CommandMetadata metadata : primary.allCommandMetadata()) {
      merged.put(metadata.getName(), metadata);
    }
    for (CommandMetadata metadata : fallback.allCommandMetadata()) {
      String name = metadata.getName();
      if (!merged.containsKey(name) && !ownedByPrimary(name)) {
        merged.put(name, metadata);
      }
    }
    return merged.values();
  }

  private boolean ownedByPrimary(String commandName) {
    int pipe = commandName.indexOf('|');
    return pipe > 0 && primary.resolve(commandName.substring(0, pipe)) != null;
  }
}
