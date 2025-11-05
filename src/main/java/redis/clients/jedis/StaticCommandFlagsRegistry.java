package redis.clients.jedis;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisByteMap;
import redis.clients.jedis.util.SafeEncoder;

import java.util.EnumSet;
import java.util.Map;

/**
 * Static implementation of CommandFlagsRegistry.
 */
public class StaticCommandFlagsRegistry implements CommandFlagsRegistry {

  // Empty flags constant for commands with no flags
  public static final EnumSet<CommandFlag> EMPTY_FLAGS = EnumSet.noneOf(CommandFlag.class);

  // Singleton instance
  private static final StaticCommandFlagsRegistry REGISTRY = createRegistry();

  private final Commands commands;

  private StaticCommandFlagsRegistry(Commands commands) {
    this.commands = commands;
  }

  /**
   * Get the singleton instance of the static command flags registry.
   * <p>
   * DO NOT USE THIS METHOD UNLESS YOU KNOW WHAT YOU ARE DOING.
   * </p>
   * @return StaticCommandFlagsRegistry
   */
  public static StaticCommandFlagsRegistry registry() {
    return REGISTRY;
  }

  private static StaticCommandFlagsRegistry createRegistry() {

    Builder builder = new Builder();

    // Delegate population to generated class
    StaticCommandFlagsRegistryInitializer.initialize(builder);

    return builder.build();
  }

  /**
   * Get the flags for a given command. Flags are looked up from a static registry based on the
   * command arguments. This approach significantly reduces memory usage by sharing flag instances
   * across all CommandObject instances.
   * <p>
   * For commands with subcommands (e.g., FUNCTION LOAD, ACL SETUSER), this method implements a
   * hierarchical lookup strategy:
   * <ol>
   * <li>First, retrieve the parent command using CommandArguments.getCommand()</li>
   * <li>Check if this is a parent command (has subcommands in the registry)</li>
   * <li>If it is a parent command, attempt to get the child/subcommand by:
   * <ul>
   * <li>Extracting the second argument from the CommandArguments object</li>
   * <li>Matching this second argument against the child items of the parent command</li>
   * </ul>
   * </li>
   * <li>Return the appropriate flags based on whether a child command was found or just use the
   * parent command's flags</li>
   * </ol>
   * @param commandArguments the command arguments containing the command and its parameters
   * @return EnumSet of CommandFlag for this command, or empty set if command has no flags
   */
  @Override
  public EnumSet<CommandFlag> getFlags(CommandArguments commandArguments) {
    // Get the parent command
    ProtocolCommand cmd = commandArguments.getCommand();
    byte[] raw = cmd.getRaw();

    // Convert to uppercase using SafeEncoder utility (faster than String.toUpperCase())
    byte[] uppercaseBytes = SafeEncoder.toUpperCase(raw);

    // Look up the parent command in the registry using byte array key
    // Object registryEntry = COMMAND_FLAGS_REGISTRY.get(uppercaseBytes);
    CommandMeta commandMeta = commands.getCommand(uppercaseBytes);

    if (commandMeta == null) {
      // Command not found in registry
      return EMPTY_FLAGS;
    }

    if (!commandMeta.hasSubcommands()) {
      // Check if this is a simple command without subcommands
      return commandMeta.getFlags();
    } else {
      // Parent command with subcommands
      // Try to extract the subcommand from the second argument
      byte[] subCommand = getSubCommand(commandArguments);
      if (subCommand != null) {
        CommandMeta subCommandMeta = commandMeta.getSubcommand(subCommand);
        if (subCommandMeta != null) {
          return subCommandMeta.getFlags();
        } else {
          // (second argument exists but not a recognized subcommand , return parent flags
          return commandMeta.getFlags();
        }
      } else {
        // no second argument (no subcommand), return parent flags
        return commandMeta.getFlags();
      }
    }
  }

  private byte[] getSubCommand(CommandArguments commandArguments) {
    if (commandArguments.size() > 1) {
      Rawable secondArg = commandArguments.get(1);
      byte[] subRaw = secondArg.getRaw();

      // Convert to uppercase using SafeEncoder utility
      return SafeEncoder.toUpperCase(subRaw);
    } else {
      return null;
    }
  }

  // Internal class to hold subcommand mappings for parent commands.
  static class Commands {

    final JedisByteMap<CommandMeta> commands = new JedisByteMap<>();

    boolean isEmpty() {
      return commands.isEmpty();
    }

    public Commands register(byte[] cmd, CommandMeta command) {
      commands.put(cmd, command);
      return this;
    }

    public boolean containsKey(byte[] command) {
      return commands.containsKey(command);
    }

    public CommandMeta getCommand(byte[] command) {
      return commands.get(command);
    }

    public Map<byte[], CommandMeta> getCommands() {
      return commands;
    }
  }

  //
  static class CommandMeta {

    final EnumSet<CommandFlag> flags;

    final Commands subcommands = new Commands();

    CommandMeta(EnumSet<CommandFlag> flags) {
      this.flags = flags;
    }

    void putSubCommand(byte[] subCommand, CommandMeta subCommandMeta) {
      this.subcommands.register(subCommand, subCommandMeta);
    }

    boolean hasSubcommands() {
      return !subcommands.isEmpty();
    }

    EnumSet<CommandFlag> getFlags() {
      if (flags == null) {
        return EMPTY_FLAGS;
      }

      return flags;
    }

    CommandMeta getSubcommand(byte[] subcommand) {
      return subcommands.getCommand(subcommand);
    }
  }

  static public class Builder {

    private final Commands commands = new Commands();

    public Builder register(String name, EnumSet<CommandFlag> flags) {
      commands.register(SafeEncoder.encode(name), new CommandMeta(flags));
      return this;
    }

    public Builder register(String name, String subcommand, EnumSet<CommandFlag> flags) {
      byte[] cmdName = SafeEncoder.encode(name);

      if (!commands.containsKey(cmdName)) {
        commands.register(SafeEncoder.encode(name), new CommandMeta(EMPTY_FLAGS));
      }

      byte[] subCmdName = SafeEncoder.encode(subcommand);
      commands.getCommand(cmdName).putSubCommand(subCmdName, new CommandMeta(flags));
      return this;
    }

    public StaticCommandFlagsRegistry build() {
      return new StaticCommandFlagsRegistry(commands);
    }
  }
}
