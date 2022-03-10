package redis.clients.jedis.resps;

import redis.clients.jedis.Builder;

import java.util.List;

import static redis.clients.jedis.BuilderFactory.STRING_LIST;
import static redis.clients.jedis.BuilderFactory.LONG;

public class CommandInfo {
  private final long arity;
  private final List<String> flags;
  private final long firstKey;
  private final long lastKey;
  private final long step;
  private final List<String> aclCategories;
  private final List<String> tips;
  private final List<String> subcommands;

  public CommandInfo(long arity, List<String> flags, long firstKey, long lastKey, long step,
      List<String> aclCategories, List<String> tips, List<String> subcommands) {
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

  public static final Builder<CommandInfo> COMMAND_INFO_BUILDER = new Builder<CommandInfo>() {
    @Override
    public CommandInfo build(Object data) {
      List<Object> commandData = (List<Object>) data;

      long arity = LONG.build(commandData.get(1));
      List<String> flags = STRING_LIST.build(commandData.get(2));
      long firstKey = LONG.build(commandData.get(3));
      long lastKey = LONG.build(commandData.get(4));
      long step = LONG.build(commandData.get(5));
      List<String> aclCategories = STRING_LIST.build(commandData.get(6));
      List<String> tips = STRING_LIST.build(commandData.get(7));
      List<String> subcommands = STRING_LIST.build(commandData.get(9));

      return new CommandInfo(arity, flags, firstKey, lastKey, step, aclCategories, tips, subcommands);
    }
  };
}
