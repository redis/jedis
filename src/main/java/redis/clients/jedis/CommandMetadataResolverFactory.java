package redis.clients.jedis;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Creates {@link CommandMetadataResolver} instances: over the metadata shipped with Jedis, over a
 * metadata file, or over individual override records.
 * @since 8.1
 */
@Experimental
public final class CommandMetadataResolverFactory {

  // Built on first use rather than in a static initializer, so a failure to construct it
  // reaches the caller (and is retried) instead of poisoning this class
  private static volatile CommandMetadataResolver DEFAULT_RESOLVER;

  private CommandMetadataResolverFactory() {
  }

  /**
   * Returns the resolver over the shared command metadata, the one client builders use when no
   * resolver is set. The metadata is the table shipped with Jedis, or the file named by the
   * {@code JEDIS_COMMAND_METADATA_PATH} environment variable when that is set. Every call returns
   * the same instance. The metadata is loaded when a command is first resolved, and a load failure
   * is raised there as an {@link IllegalStateException}.
   * @return the shared default resolver
   * @since 8.1
   */
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

  /**
   * Creates a resolver over the metadata in the given file, which must have the layout of
   * {@code CommandMetadata.json}. The file is read when this method is called. The known server
   * metadata corrections Jedis applies to its own metadata are applied to the loaded records as
   * well. The resolver knows only the commands in the file; combine it with
   * {@link CommandMetadataResolver#withFallback(CommandMetadataResolver)} to cover the rest.
   * @param metadataFile the metadata file to read
   * @return a resolver over the commands in the file
   * @throws IllegalArgumentException if the file is null
   * @throws IllegalStateException if the file cannot be read or parsed, or a subcommand in it is
   *           not keyed by its full {@code PARENT|CHILD} name
   * @since 8.1
   */
  public static CommandMetadataResolver fromFile(Path metadataFile) {
    JedisAsserts.notNull(metadataFile, "metadataFile must not be null");
    Map<String, CommandMetadata> table = MetadataReader.read(metadataFile);
    validateSubcommandLayout(metadataFile, table);
    return new TableBackedResolver(MetadataResolver.prepareTable(table));
  }

  /**
   * Creates a resolver over the given metadata records, typically override records that correct
   * single commands or describe commands the shipped metadata does not know. The records are used
   * as given, and the resolver knows only these commands and their subcommands; combine it with
   * {@link CommandMetadataResolver#withFallback(CommandMetadataResolver)} to cover the rest.
   * @param records the metadata records; a subcommand is supplied through its container command's
   *          record, not as a record of its own
   * @return a resolver over the given records
   * @throws IllegalArgumentException if the array or a record is null, a record is a subcommand, or
   *           two records share a command name
   * @since 8.1
   */
  public static CommandMetadataResolver of(CommandMetadata... records) {
    JedisAsserts.notNull(records, "records must not be null");
    return of(Arrays.asList(records));
  }

  /**
   * Creates a resolver over the given collection of metadata records, typically override records
   * that correct single commands or describe commands the shipped metadata does not know. The
   * records are used as given, and the resolver knows only these commands and their subcommands;
   * combine it with {@link CommandMetadataResolver#withFallback(CommandMetadataResolver)} to cover
   * the rest.
   * @param records the metadata records; a subcommand is supplied through its container command's
   *          record, not as a record of its own
   * @return a resolver over the given records
   * @throws IllegalArgumentException if the collection or a record is null, a record is a
   *           subcommand, or two records share a command name
   * @since 8.1
   */
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
      return MetadataResolver.lookupIgnoreCase(table, commandName);
    }

    @Override
    public Collection<CommandMetadata> allCommandMetadata() {
      return table.values();
    }
  }
}
