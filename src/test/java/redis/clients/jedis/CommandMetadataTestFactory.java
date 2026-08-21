package redis.clients.jedis;

import java.util.Set;

import redis.clients.jedis.MetadataResolver.CommandMetadata;

/**
 * Test-only bridge for constructing {@link CommandMetadata} outside this package; production
 * construction stays package-private (generated table, reader, and fixes only).
 */
public final class CommandMetadataTestFactory {

  private CommandMetadataTestFactory() {
  }

  public static CommandMetadata create(String name, Set<String> flags, Set<String> tips,
      long firstKey, long lastKey, long step, boolean hasKeyNameSpec) {
    return new CommandMetadata(name, flags, tips, firstKey, lastKey, step, hasKeyNameSpec);
  }
}
