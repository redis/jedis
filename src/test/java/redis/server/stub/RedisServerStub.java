package redis.server.stub;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.util.SafeEncoder;
import redis.server.stub.command.server.AuthCommand;
import redis.server.stub.command.server.ClientCommand;
import redis.server.stub.command.server.HelloCommand;
import redis.server.stub.command.server.PingCommand;
import redis.server.stub.command.string.GetCommand;
import redis.server.stub.command.string.SetCommand;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stub Redis server implementation for testing. Provides a minimal in-memory Redis implementation
 * with basic commands for test scenarios.
 * <p>
 * <b>Protocol Support</b>: This implementation only supports RESP3 protocol. Clients must use RESP3
 * or the HELLO 3 command will fail with NOPROTO error.
 * <p>
 * Design: - Extends TcpMockServer for TCP handling - Overrides createCommandProcessor() for command
 * processing - Single-threaded command executor (all commands run sequentially) - CommandRegistry
 * for command resolution and execution - Binary-safe keys and values - Primitive storage operations
 * <p>
 * Thread safety: - Socket I/O: TcpMockServer handles multiple client connections - Command
 * execution: Single-threaded executor ensures sequential processing - No synchronization needed in
 * RedisDataStore (single-threaded access)
 */
public class RedisServerStub extends TcpMockServer {

  // Single-threaded executor for command processing
  private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

  // Data store (accessed only from commandExecutor thread)
  private final RedisDataStore dataStore = new RedisDataStore();

  // Command registry
  private final CommandRegistry commandRegistry;

  // Interceptor registry: command name → list of interceptors
  // Use CopyOnWriteArrayList for thread-safe registration (though registration happens on test
  // thread)
  private final Map<String, List<CommandInterceptor>> interceptors = new ConcurrentHashMap<>();

  public RedisServerStub() {
    // Initialize command registry
    this.commandRegistry = new CommandRegistry(dataStore, this);

    // Register all MVP commands
    registerCommands();
  }

  /**
   * Register all built-in commands.
   */
  private void registerCommands() {
    // String commands
    commandRegistry.register(new GetCommand());
    commandRegistry.register(new SetCommand());

    // Server commands
    commandRegistry.register(new PingCommand());
    commandRegistry.register(new HelloCommand());
    commandRegistry.register(new AuthCommand());
    commandRegistry.register(new ClientCommand());
  }

  @Override
  protected String processCommand(CommandArguments args, ClientState clientState) {

    try {
      return commandExecutor.submit(() -> {
        executeInterceptors(args, clientState);

        return commandRegistry.execute(args, clientState);
      }).get();
    } catch (Exception e) {
      return "-ERR " + e.getMessage() + "\r\n";
    }
  }

  /**
   * Get the data store (for testing).
   */
  public RedisDataStore getDataStore() {
    return dataStore;
  }

  @Override
  public void stop() throws IOException {
    // Shutdown command executor
    commandExecutor.shutdown();

    // Stop TCP server
    super.stop();
  }

  // ========== Command Interceptor API ==========

  /**
   * Register interceptor for specific command. Interceptor runs BEFORE command execution on
   * executor thread.
   * @param commandName command to intercept (e.g., "PUBLISH", "GET") Case-insensitive, will be
   *          converted to uppercase
   * @param interceptor code to run before execution
   */
  public void beforeCommand(String commandName, CommandInterceptor interceptor) {
    if (commandName == null || interceptor == null) {
      throw new IllegalArgumentException("Command name and interceptor cannot be null");
    }

    String normalizedCommand = commandName.toUpperCase();
    interceptors.computeIfAbsent(normalizedCommand, k -> new CopyOnWriteArrayList<>())
        .add(interceptor);
  }

  /**
   * Register interceptor for ALL commands. Interceptor runs before every command execution.
   * @param interceptor code to run before any command
   */
  public void beforeAnyCommand(CommandInterceptor interceptor) {
    beforeCommand("*", interceptor);
  }

  /**
   * Clear all registered interceptors. Useful for test cleanup.
   */
  public void clearInterceptors() {
    interceptors.clear();
  }

  // ========== Private: Interceptor Execution ==========

  /**
   * Execute all registered interceptors for this command. Runs on command executor thread
   * (single-threaded, thread-safe).
   * @param args command arguments
   * @param clientState client executing the command
   * @throws Exception any exception from interceptor propagates (fails test)
   */
  private void executeInterceptors(CommandArguments args, ClientState clientState)
      throws Exception {
    String commandName = SafeEncoder.encode(args.getCommand().getRaw()).toUpperCase();

    // Create context for interceptors
    CommandContext ctx = new CommandContextImpl(clientState, dataStore, this);

    // Execute command-specific interceptors
    List<CommandInterceptor> commandInterceptors = interceptors.get(commandName);
    if (commandInterceptors != null) {
      for (CommandInterceptor interceptor : commandInterceptors) {
        // NO try-catch - let ALL exceptions propagate
        // If interceptor fails, test should fail
        interceptor.beforeExecute(args, ctx);
      }
    }

    // Execute global interceptors (registered with "*")
    List<CommandInterceptor> globalInterceptors = interceptors.get("*");
    if (globalInterceptors != null) {
      for (CommandInterceptor interceptor : globalInterceptors) {
        interceptor.beforeExecute(args, ctx);
      }
    }
  }

}
