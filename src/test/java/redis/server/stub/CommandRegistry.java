package redis.server.stub;

import redis.clients.jedis.CommandArguments;
import redis.server.stub.command.RedisCommand;
import redis.server.stub.command.CommandContext;
import redis.server.stub.command.CommandContextImpl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for Redis commands. Responsibilities: - Register command implementations - Resolve
 * commands by name (top-level) - Resolve subcommands via command.resolve() - Delegate execution to
 * commands Commands are responsible for: - Validating arguments - Implementing business logic -
 * Returning RESP-formatted responses
 */
public class CommandRegistry {

  // Registry: command name -> command implementation
  private final Map<String, RedisCommand> commands = new HashMap<>();

  // Dependencies injected into commands via context
  private final RedisDataStore dataStore;
  private final RedisServerStub server;

  public CommandRegistry(RedisDataStore dataStore, RedisServerStub server) {
    this.dataStore = dataStore;
    this.server = server;
  }

  /**
   * Register a command implementation.
   * @param command the command to register
   */
  public void register(RedisCommand command) {
    commands.put(command.getName().toUpperCase(), command);
  }

  /**
   * Resolve command by name (case-insensitive). Returns null if command not found.
   * @param commandName the command name
   * @return the command, or null if not found
   */
  public RedisCommand resolve(String commandName) {
    return commands.get(commandName.toUpperCase());
  }

  /**
   * Execute a command by name with arguments. Flow: 1. Resolve top-level command 2. If has
   * subcommands, resolve subcommand (like C# cmd.Resolve) 3. Delegate to command.execute()
   * @param args command arguments
   * @param client client state
   * @param clientHandler client handler for this connection
   * @return RESP-formatted response
   */
  public String execute(CommandArguments args, ClientState client, ClientHandler clientHandler) {
    // Step 1: Resolve top-level command
    // Command is at index 0, getRaw() returns the command name bytes
    String commandName = new String(args.getCommand().getRaw(), StandardCharsets.UTF_8);
    RedisCommand command = resolve(commandName);
    if (command == null) {
      return "-ERR unknown command '" + commandName.toLowerCase() + "'\r\n";
    }

    // Step 2: If command has subcommands, resolve the subcommand (like C# cmd.Resolve)
    if (command.hasSubCommands()) {
      RedisCommand subcommand = command.resolve(args);
      if (subcommand != null) {
        command = subcommand; // Use resolved subcommand
      }
      // else: null means use parent's execute() for error
    }

    // Step 3: Create context and delegate to command
    CommandContext ctx = new CommandContextImpl(client, dataStore, server, clientHandler);

    try {
      return command.execute(args, ctx);
    } catch (Exception e) {
      return "-ERR " + e.getMessage() + "\r\n";
    }
  }
}
