package redis.clients.jedis;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Metadata of one Redis command, in the normalized shape of the server's {@code COMMAND} reply: its
 * name, command flags, command tips, key positions and container subcommands. Jedis derives
 * client-side caching eligibility and the cluster command flags and request/response policies from
 * these records.
 * <p>
 * Names are upper case, with container subcommands named {@code PARENT|CHILD} (for example
 * {@code XINFO|STREAM}); flags and tips are lower case, as the server reports them. A record
 * obtained from a {@link CommandMetadataResolver} or from {@link Builder#build()} is immutable.
 * Create records with {@link #builder(String)}, or derive a corrected copy of an existing one with
 * {@link #toBuilder()}.
 * @since 8.1
 */
@Experimental
public final class CommandMetadata {

  private final String name;
  private Set<String> flags;
  private Set<String> tips;
  private final long firstKey;
  private final long lastKey;
  private final long step;
  private final boolean hasKeyNameSpec;

  private Set<CommandMetadata> subcommands = new HashSet<>();

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
    subcommands = Collections.unmodifiableSet(subcommands);
  }

  void addSubcommand(CommandMetadata subcommand) {
    subcommands.add(subcommand);
  }

  /**
   * Returns the metadata of this container command's subcommands, each named {@code PARENT|CHILD}.
   * @return the subcommand records; empty when the command has no subcommands
   * @since 8.1
   */
  public Set<CommandMetadata> getSubcommands() {
    return subcommands;
  }

  /**
   * Returns the command name in upper case; a container subcommand carries its full
   * {@code PARENT|CHILD} name.
   * @return the command name
   * @since 8.1
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the command flags in lower case, for example {@code readonly}, {@code write} or
   * {@code blocking}.
   * @return the command flags; empty when the command has none
   * @since 8.1
   */
  public Set<String> getFlags() {
    return flags;
  }

  /**
   * Returns the command tips in lower case, for example {@code nondeterministic_output},
   * {@code dont_cache} or {@code request_policy:all_shards}.
   * @return the command tips; empty when the command has none
   * @since 8.1
   */
  public Set<String> getTips() {
    return tips;
  }

  /**
   * Tells whether the command carries the given command flag.
   * @param flag the flag name in lower case, for example {@code readonly}
   * @return true if the command has the flag
   * @since 8.1
   */
  public boolean hasFlag(String flag) {
    return flags.contains(flag);
  }

  /**
   * Tells whether the command carries the given command tip.
   * @param tip the tip in lower case, for example {@code dont_cache}
   * @return true if the command has the tip
   * @since 8.1
   */
  public boolean hasTip(String tip) {
    return tips.contains(tip);
  }

  /**
   * Returns the position of the first key argument, as the {@code COMMAND} reply reports it.
   * @return the first key position; 0 when the command declares no key positions
   * @since 8.1
   */
  public long getFirstKey() {
    return firstKey;
  }

  /**
   * Returns the position of the last key argument, as the {@code COMMAND} reply reports it. A
   * negative value counts from the end of the arguments.
   * @return the last key position
   * @since 8.1
   */
  public long getLastKey() {
    return lastKey;
  }

  /**
   * Returns the distance between two consecutive key arguments, as the {@code COMMAND} reply
   * reports it.
   * @return the key step; 0 when the command declares no key positions
   * @since 8.1
   */
  public long getStep() {
    return step;
  }

  /**
   * Tells whether the command has a key specification that names a key, that is one without the
   * {@code not_key} flag. Together with the key positions this is how a command proves that it
   * takes a key.
   * @return true if a key specification of the command names a key
   * @since 8.1
   */
  public boolean hasKeyNameSpec() {
    return hasKeyNameSpec;
  }

  /**
   * Starts a metadata record for the given command, with no flags, tips, key positions or
   * subcommands.
   * @param name the command name, converted to upper case; a container subcommand is named
   *          {@code PARENT|CHILD}
   * @return a builder for the record
   * @throws IllegalArgumentException if the name is null or blank
   * @since 8.1
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  /**
   * Starts a metadata record that begins as a copy of this one, to derive a corrected version of an
   * existing command, for example
   * {@code resolver.resolve("GET").toBuilder().tips("dont_cache").build()}. This record is not
   * changed.
   * @return a builder holding this record's name, flags, tips, key positions and subcommands
   * @since 8.1
   */
  public Builder toBuilder() {
    Builder builder = new Builder(name);
    builder.flags.addAll(flags);
    builder.tips.addAll(tips);
    builder.firstKey = firstKey;
    builder.lastKey = lastKey;
    builder.step = step;
    builder.hasKeyNameSpec = hasKeyNameSpec;
    for (CommandMetadata subcommand : subcommands) {
      builder.subcommands.put(subcommand.getName(), subcommand);
    }
    return builder;
  }

  /**
   * Builds an immutable {@link CommandMetadata} record. Flags and tips are trimmed and converted to
   * lower case, so they match the form the eligibility rules and the flags registry expect.
   * @since 8.1
   */
  @Experimental
  public static final class Builder {

    private final String name;
    private final Set<String> flags = new HashSet<>();
    private final Set<String> tips = new HashSet<>();
    private long firstKey;
    private long lastKey;
    private long step;
    private boolean hasKeyNameSpec;
    private final Map<String, CommandMetadata> subcommands = new LinkedHashMap<>();

    private Builder(String name) {
      JedisAsserts.notNull(name, "name must not be null");
      JedisAsserts.isTrue(!name.trim().isEmpty(), "name must not be empty");
      this.name = name.trim().toUpperCase(Locale.ENGLISH);
    }

    /**
     * Adds command flags to the record, for example {@code readonly}.
     * @param flags the flags to add; converted to lower case
     * @return this builder
     * @throws IllegalArgumentException if the array or one of its elements is null
     * @since 8.1
     */
    public Builder flags(String... flags) {
      addNormalized(this.flags, flags, "flags");
      return this;
    }

    /**
     * Removes command flags from the record; a flag the record does not have is ignored.
     * @param flags the flags to remove; converted to lower case
     * @return this builder
     * @throws IllegalArgumentException if the array or one of its elements is null
     * @since 8.1
     */
    public Builder removeFlags(String... flags) {
      removeNormalized(this.flags, flags, "flags");
      return this;
    }

    /**
     * Adds command tips to the record, for example {@code dont_cache}.
     * @param tips the tips to add; converted to lower case
     * @return this builder
     * @throws IllegalArgumentException if the array or one of its elements is null
     * @since 8.1
     */
    public Builder tips(String... tips) {
      addNormalized(this.tips, tips, "tips");
      return this;
    }

    /**
     * Removes command tips from the record; a tip the record does not have is ignored.
     * @param tips the tips to remove; converted to lower case
     * @return this builder
     * @throws IllegalArgumentException if the array or one of its elements is null
     * @since 8.1
     */
    public Builder removeTips(String... tips) {
      removeNormalized(this.tips, tips, "tips");
      return this;
    }

    /**
     * Sets where the key arguments are, in the form the {@code COMMAND} reply uses. A command with
     * a first key position and a step above zero is treated as taking a key.
     * @param firstKey position of the first key argument; 0 for none
     * @param lastKey position of the last key argument; negative values count from the end
     * @param step distance between two consecutive key arguments; 0 for none
     * @return this builder
     * @since 8.1
     */
    public Builder keyPositions(long firstKey, long lastKey, long step) {
      this.firstKey = firstKey;
      this.lastKey = lastKey;
      this.step = step;
      return this;
    }

    /**
     * Sets whether the command has a key specification that names a key, that is one without the
     * {@code not_key} flag. A command for which this is true is treated as taking a key.
     * @param hasKeyNameSpec true if a key specification of the command names a key
     * @return this builder
     * @since 8.1
     */
    public Builder keyNameSpec(boolean hasKeyNameSpec) {
      this.hasKeyNameSpec = hasKeyNameSpec;
      return this;
    }

    /**
     * Adds the metadata of a subcommand of this container command. A subcommand added earlier under
     * the same name is replaced.
     * @param subcommand the subcommand record, named {@code PARENT|CHILD} with this record's name
     *          as the parent
     * @return this builder
     * @throws IllegalArgumentException if the record is null or its name does not start with this
     *           record's name followed by {@code |}
     * @since 8.1
     */
    public Builder subcommand(CommandMetadata subcommand) {
      JedisAsserts.notNull(subcommand, "subcommand must not be null");
      JedisAsserts.isTrue(subcommand.getName().startsWith(name + "|"),
        "subcommand '" + subcommand.getName() + "' of '" + name
            + "' must be named by its full PARENT|CHILD name");
      subcommands.put(subcommand.getName(), subcommand);
      return this;
    }

    /**
     * Creates the immutable metadata record from the values set on this builder.
     * @return the metadata record
     * @since 8.1
     */
    public CommandMetadata build() {
      CommandMetadata metadata = new CommandMetadata(name, new HashSet<>(flags),
          new HashSet<>(tips), firstKey, lastKey, step, hasKeyNameSpec);
      subcommands.values().forEach(metadata::addSubcommand);
      metadata.seal();
      return metadata;
    }

    private static void addNormalized(Set<String> target, String[] values, String what) {
      target.addAll(normalize(values, what));
    }

    private static void removeNormalized(Set<String> target, String[] values, String what) {
      target.removeAll(normalize(values, what));
    }

    private static Set<String> normalize(String[] values, String what) {
      JedisAsserts.notNull(values, what + " must not be null");
      Set<String> normalized = new HashSet<>();
      for (String value : Arrays.asList(values)) {
        JedisAsserts.notNull(value, what + " must not contain null elements");
        normalized.add(value.trim().toLowerCase(Locale.ENGLISH));
      }
      return normalized;
    }
  }
}
