package redis.clients.jedis.resps;

import java.util.List;

public class CommandInfo {
  private final String name;
  private final long arity;
  private final List<String> flags;
  private final long firstKey;
  private final long lastKey;
  private final long step;
  private final List<String> aclCategories;
  private final List<String> tips;
  private final List<String> subcommands;

  public CommandInfo(String name, long arity, List<String> flags, long firstKey, long lastKey, long step,
      List<String> aclCategories, List<String> tips, List<String> subcommands) {
    this.name = name;
    this.arity = arity;
    this.flags = flags;
    this.firstKey = firstKey;
    this.lastKey = lastKey;
    this.step = step;
    this.aclCategories = aclCategories;
    this.tips = tips;
    this.subcommands = subcommands;
  }

  /**
   * Command's name in lowercase
   */
  public String getName() {
    return name;
  }

  /**
   * Arity is the number of arguments a command expects. It follows a simple pattern:
   * A positive integer means a fixed number of arguments.
   * A negative integer means a minimal number of arguments.
   *
   * Examples:
   *
   * GET's arity is 2 since the command only accepts one argument and always has the format GET _key_.
   * MGET's arity is -2 since the command accepts at least one argument, but possibly multiple ones: MGET _key1_ [key2] [key3] ....
   */
  public long getArity() {
    return arity;
  }

  /**
   * Command flags
   */
  public List<String> getFlags() {
    return flags;
  }

  /**
   * The position of the command's first key name argument
   */
  public long getFirstKey() {
    return firstKey;
  }

  /**
   * The position of the command's last key name argument
   * Commands that accept a single key have both first key and last key set to 1
   */
  public long getLastKey() {
    return lastKey;
  }

  /**
   * This value is the step, or increment, between the first key and last key values where the keys are
   */
  public long getStep() {
    return step;
  }

  /**
   * An array of simple strings that are the ACL categories to which the command belongs
   */
  public List<String> getAclCategories() {
    return aclCategories;
  }

  /**
   * Helpful information about the command
   */
  public List<String> getTips() {
    return tips;
  }

  /**
   * All the command's subcommands, if any
   */
  public List<String> getSubcommands() {
    return subcommands;
  }
}
