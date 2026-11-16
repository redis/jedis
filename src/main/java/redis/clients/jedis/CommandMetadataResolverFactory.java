package redis.clients.jedis;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.JedisAsserts;

@Experimental
public final class CommandMetadataResolverFactory {

  // Built on first use rather than in a static initializer, so a failure to construct it
  // reaches the caller (and is retried) instead of poisoning this class
  private static volatile CommandMetadataResolver DEFAULT_RESOLVER;

  private CommandMetadataResolverFactory() {
  }

  public static CommandMetadataResolver defaultResolver() {
    CommandMetadataResolver resolver = DEFAULT_RESOLVER;
    if (resolver == null) {
      synchronized (CommandMetadataResolverFactory.class) {
        resolver = DEFAULT_RESOLVER;
        if (resolver == null) {
          resolver = new MetadataResolver();
          DEFAULT_RESOLVER = resolver;
        }
      }
    }
    return resolver;
  }

  public static CommandMetadataResolver fromFile(Path metadataFile) {
    JedisAsserts.notNull(metadataFile, "metadataFile must not be null");
    Map<String, CommandMetadata> table = MetadataReader.read(metadataFile);
    validateSubcommandLayout(metadataFile, table);
    return new TableBackedResolver(MetadataResolver.prepareTable(table));
  }

  public static CommandMetadataResolver of(CommandMetadata... records) {
    JedisAsserts.notNull(records, "records must not be null");
    return of(Arrays.asList(records));
  }

  public static CommandMetadataResolver of(Collection<CommandMetadata> records) {
    JedisAsserts.notNull(records, "records must not be null");
    Map<String, CommandMetadata> table = new LinkedHashMap<>();
    for (CommandMetadata record : records) {
      JedisAsserts.notNull(record, "records must not contain null elements");
      // consumers reach a subcommand through its container's record, so one supplied on its own
      // would silently never take effect
      JedisAsserts.isTrue(record.getName().indexOf('|') < 0, "subcommand '" + record.getName()
          + "' must be supplied through its container command's record");
      putUnique(table, record);
      for (CommandMetadata subcommand : record.getSubcommands()) {
        putUnique(table, subcommand);
      }
    }
    return new TableBackedResolver(Collections.unmodifiableMap(table));
  }

  private static void putUnique(Map<String, CommandMetadata> table, CommandMetadata record) {
    JedisAsserts.isTrue(table.put(record.getName(), record) == null,
      "duplicate metadata record for command '" + record.getName() + "'");
  }

  private static void validateSubcommandLayout(Path metadataFile,
      Map<String, CommandMetadata> table) {
    for (CommandMetadata metadata : table.values()) {
      for (CommandMetadata subcommand : metadata.getSubcommands()) {
        if (!subcommand.getName().startsWith(metadata.getName() + "|")) {
          throw new IllegalStateException("Failed to read command metadata from " + metadataFile
              + ": subcommand '" + subcommand.getName() + "' of '" + metadata.getName()
              + "' must be keyed by its full PARENT|CHILD name");
        }
      }
    }
  }

  private static final class TableBackedResolver implements CommandMetadataResolver {

    private final Map<String, CommandMetadata> table;

    private TableBackedResolver(Map<String, CommandMetadata> table) {
      this.table = table;
    }

    @Override
    public CommandMetadata resolve(String commandName) {
      return table.get(commandName);
    }

    @Override
    public Collection<CommandMetadata> allCommandMetadata() {
      return table.values();
    }
  }
}
