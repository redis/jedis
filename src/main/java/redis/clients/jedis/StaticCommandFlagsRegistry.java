package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisByteMap;
import redis.clients.jedis.util.SafeEncoder;

import java.util.EnumSet;
import java.util.Map;

/**
 * Static implementation of CommandFlagsRegistry.
 */
@Internal
public class StaticCommandFlagsRegistry implements CommandFlagsRegistry {

  // Empty flags constant for commands with no flags
  public static final EnumSet<CommandFlag> EMPTY_FLAGS = EnumSet.noneOf(CommandFlag.class);

  // Default request policy for commands without a specific policy
  public static final RequestPolicy DEFAULT_REQUEST_POLICY = RequestPolicy.DEFAULT;

  // Default response policy for commands without a specific policy
  public static final ResponsePolicy DEFAULT_RESPONSE_POLICY = ResponsePolicy.DEFAULT;

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
   * Uses the same hierarchical lookup strategy as {@link #lookupCommandMeta(CommandArguments)}.
   * @param commandArguments the command arguments containing the command and its parameters
   * @return EnumSet of CommandFlag for this command, or empty set if command has no flags
   */
  @Override
  public EnumSet<CommandFlag> getFlags(CommandArguments commandArguments) {
    CommandMeta commandMeta = lookupCommandMeta(commandArguments);
    if (commandMeta == null) {
      return EMPTY_FLAGS;
    }
    return commandMeta.getFlags();
  }

  /**
   * Get the request policy for a given command. The request policy helps clients determine which
   * shards to send the command to in a clustered deployment.
   * <p>
   * Uses the same hierarchical lookup strategy as {@link #getFlags(CommandArguments)}.
   * @param commandArguments the command arguments containing the command and its parameters
   * @return RequestPolicy for this command, or DEFAULT if no specific policy is defined
   */
  @Override
  public RequestPolicy getRequestPolicy(CommandArguments commandArguments) {
    CommandMeta commandMeta = lookupCommandMeta(commandArguments);
    if (commandMeta == null) {
      return DEFAULT_REQUEST_POLICY;
    }
    return commandMeta.getRequestPolicy();
  }

  /**
   * Get the response policy for a given command. The response policy helps clients determine how to
   * aggregate replies from multiple shards in a cluster.
   * <p>
   * Uses the same hierarchical lookup strategy as {@link #getFlags(CommandArguments)}.
   * @param commandArguments the command arguments containing the command and its parameters
   * @return ResponsePolicy for this command, or DEFAULT if no specific policy is defined
   */
  @Override
  public ResponsePolicy getResponsePolicy(CommandArguments commandArguments) {
    CommandMeta commandMeta = lookupCommandMeta(commandArguments);
    if (commandMeta == null) {
      return DEFAULT_RESPONSE_POLICY;
    }
    return commandMeta.getResponsePolicy();
  }

  /**
   * Common lookup logic for finding the CommandMeta for a given command. Handles both simple
   * commands and commands with subcommands.
   */
  private CommandMeta lookupCommandMeta(CommandArguments commandArguments) {
    ProtocolCommand cmd = commandArguments.getCommand();
    byte[] raw = cmd.getRaw();
    byte[] uppercaseBytes = SafeEncoder.toUpperCase(raw);

    CommandMeta commandMeta = commands.getCommand(uppercaseBytes);
    if (commandMeta == null) {
      return null;
    }

    if (commandMeta.hasSubcommands()) {
      byte[] subCommand = getSubCommand(commandArguments);
      if (subCommand != null) {
        CommandMeta subCommandMeta = commandMeta.getSubcommand(subCommand);
        if (subCommandMeta != null) {
          return subCommandMeta;
        }
      }
    }
    return commandMeta;
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

  /**
   * Internal class to hold command metadata including flags and policies.
   */
  static class CommandMeta {

    final EnumSet<CommandFlag> flags;
    final RequestPolicy requestPolicy;
    final ResponsePolicy responsePolicy;
    final Commands subcommands = new Commands();

    CommandMeta(EnumSet<CommandFlag> flags) {
      this(flags, DEFAULT_REQUEST_POLICY, DEFAULT_RESPONSE_POLICY);
    }

    CommandMeta(EnumSet<CommandFlag> flags, RequestPolicy requestPolicy,
        ResponsePolicy responsePolicy) {
      this.flags = flags;
      this.requestPolicy = requestPolicy != null ? requestPolicy : DEFAULT_REQUEST_POLICY;
      this.responsePolicy = responsePolicy != null ? responsePolicy : DEFAULT_RESPONSE_POLICY;
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

    RequestPolicy getRequestPolicy() {
      return requestPolicy;
    }

    ResponsePolicy getResponsePolicy() {
      return responsePolicy;
    }

    CommandMeta getSubcommand(byte[] subcommand) {
      return subcommands.getCommand(subcommand);
    }
  }

  /**
   * Builder for constructing StaticCommandFlagsRegistry instances.
   */
  static public class Builder {

    private final Commands commands = new Commands();

    public Builder register(String name, EnumSet<CommandFlag> flags) {
      commands.register(SafeEncoder.encode(name), new CommandMeta(flags));
      return this;
    }

    public Builder register(String name, EnumSet<CommandFlag> flags, RequestPolicy requestPolicy,
        ResponsePolicy responsePolicy) {
      commands.register(SafeEncoder.encode(name),
        new CommandMeta(flags, requestPolicy, responsePolicy));
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

    public Builder register(String name, String subcommand, EnumSet<CommandFlag> flags,
        RequestPolicy requestPolicy, ResponsePolicy responsePolicy) {
      byte[] cmdName = SafeEncoder.encode(name);

      if (!commands.containsKey(cmdName)) {
        commands.register(SafeEncoder.encode(name), new CommandMeta(EMPTY_FLAGS));
      }

      byte[] subCmdName = SafeEncoder.encode(subcommand);
      commands.getCommand(cmdName).putSubCommand(subCmdName,
        new CommandMeta(flags, requestPolicy, responsePolicy));
      return this;
    }

    public StaticCommandFlagsRegistry build() {
      return new StaticCommandFlagsRegistry(commands);
    }
  }
}
