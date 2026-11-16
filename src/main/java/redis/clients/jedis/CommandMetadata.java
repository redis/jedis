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

  public Set<CommandMetadata> getSubcommands() {
    return subcommands;
  }

  public String getName() {
    return name;
  }

  public Set<String> getFlags() {
    return flags;
  }

  public Set<String> getTips() {
    return tips;
  }

  public boolean hasFlag(String flag) {
    return flags.contains(flag);
  }

  public boolean hasTip(String tip) {
    return tips.contains(tip);
  }

  public long getFirstKey() {
    return firstKey;
  }

  public long getLastKey() {
    return lastKey;
  }

  public long getStep() {
    return step;
  }

  public boolean hasKeyNameSpec() {
    return hasKeyNameSpec;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

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

    public Builder flags(String... flags) {
      addNormalized(this.flags, flags, "flags");
      return this;
    }

    public Builder removeFlags(String... flags) {
      removeNormalized(this.flags, flags, "flags");
      return this;
    }

    public Builder tips(String... tips) {
      addNormalized(this.tips, tips, "tips");
      return this;
    }

    public Builder removeTips(String... tips) {
      removeNormalized(this.tips, tips, "tips");
      return this;
    }

    public Builder keyPositions(long firstKey, long lastKey, long step) {
      this.firstKey = firstKey;
      this.lastKey = lastKey;
      this.step = step;
      return this;
    }

    public Builder keyNameSpec(boolean hasKeyNameSpec) {
      this.hasKeyNameSpec = hasKeyNameSpec;
      return this;
    }

    public Builder subcommand(CommandMetadata subcommand) {
      JedisAsserts.notNull(subcommand, "subcommand must not be null");
      JedisAsserts.isTrue(subcommand.getName().startsWith(name + "|"), "subcommand '"
          + subcommand.getName() + "' of '" + name + "' must be named by its full PARENT|CHILD name");
      subcommands.put(subcommand.getName(), subcommand);
      return this;
    }

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
