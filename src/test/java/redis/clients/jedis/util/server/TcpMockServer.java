package redis.clients.jedis.util.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple TCP mock server for testing Redis push notifications and timeout behavior. This server
 * can accept connections and send predefined responses including push messages.
 */
public class TcpMockServer {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
  Logger logger = LoggerFactory.getLogger(TcpMockServer.class);
  private ServerSocket serverSocket;
  private int port;
  private CommandHandler commandHandler;

  /**
   * Start the server on an available port
   */
  public void start() throws IOException {
    start(0); // Use any available port
  }

  /**
   * Start the server on a specific port
   */
  public void start(int port) throws IOException {
    serverSocket = new ServerSocket(port);
    this.port = serverSocket.getLocalPort();
    running.set(true);

    executor.submit(() -> {
      while (running.get() && !serverSocket.isClosed()) {
        try {
          Socket clientSocket = serverSocket.accept();
          executor.submit(new ClientHandler(clientSocket));
        } catch (IOException e) {
          if (running.get()) {
            logger.error("Error accepting client connection: " + e.getMessage());
          }
        }
      }
    });
  }

  /**
   * Stop the server and close all active connections
   */
  public void stop() throws IOException {
    running.set(false);

    // Close all active client connections first
    closeAllActiveConnections();

    // Close the server socket
    if (serverSocket != null && !serverSocket.isClosed()) {
      serverSocket.close();
    }
    executor.shutdownNow();
  }

  /**
   * Get the port the server is running on
   */
  public int getPort() {
    return port;
  }

  /**
   * Check if the server is running
   */
  public boolean isRunning() {
    return running.get() && serverSocket != null && !serverSocket.isClosed();
  }

  /**
   * Get the number of connected clients
   */
  public int getConnectedClientCount() {
    return connectedClients.size();
  }

  /**
   * Generic method to send a push message to all connected clients.
   * @param pushType the type of push message (e.g., "MIGRATING", "MIGRATED")
   * @param args optional arguments for the push message
   */
  public void sendPushMessageToAll(String pushType, String... args) {
    connectedClients.values().forEach(client -> client.sendPushMessage(pushType, args));
  }

  /**
   * Send a MIGRATING push message to all connected clients
   */
  public void sendMigratingPushToAll() {
    sendPushMessageToAll("MIGRATING", "30"); // Default slot 30
  }

  /**
   * Send a MIGRATED push message to all connected clients
   */
  public void sendMigratedPushToAll() {
    sendPushMessageToAll("MIGRATED");
  }

  /**
   * Send a FAILING_OVER push message to all connected clients
   */
  public void sendFailingOverPushToAll() {
    sendPushMessageToAll("FAILING_OVER", "30"); // Default slot 30
  }

  /**
   * Send a FAILED_OVER push message to all connected clients
   */
  public void sendFailedOverPushToAll() {
    sendPushMessageToAll("FAILED_OVER");
  }

  public void sendMovingPushToAll(String targetHost) {
    sendPushMessageToAll("MOVING", "30", targetHost);
  }

  /**
   * Get the current command handler.
   * @return The current command handler, or null if none is set
   */
  public CommandHandler getCommandHandler() {
    return commandHandler;
  }

  /**
   * Set a custom command handler for processing Redis commands.
   * @param commandHandler The command handler to use, or null to use only built-in handlers
   */
  public void setCommandHandler(CommandHandler commandHandler) {
    this.commandHandler = commandHandler;
  }

  /**
   * Close all active client connections
   */
  private void closeAllActiveConnections() {
    // Create a copy of the values to avoid ConcurrentModificationException
    java.util.List<ClientHandler> clientsToClose = new java.util.ArrayList<>(
        connectedClients.values());

    for (ClientHandler client : clientsToClose) {
      try {
        client.forceClose();
      } catch (Exception e) {
        logger.error("Error closing client connection: " + e.getMessage());
      }
    }

    // Clear the map
    connectedClients.clear();
  }

  /**
   * Static registry of built-in command responses (shared across all client handlers). Commands are
   * stored as CommandKey (command + optional subcommand) for smart lookup.
   */
  private static final java.util.Map<CommandKey, String> BUILTIN_RESPONSES;

  static {
    java.util.Map<CommandKey, String> responses = new java.util.HashMap<>();

    // RESP3 HELLO response - version 7.4.0 to support client-side caching
    responses.put(new CommandKey(Protocol.Command.HELLO),
      "%7\r\n" + "$6\r\nserver\r\n$5\r\nredis\r\n" + "$7\r\nversion\r\n$5\r\n7.4.0\r\n"
          + "$5\r\nproto\r\n:3\r\n" + "$2\r\nid\r\n:1\r\n" + "$4\r\nmode\r\n$10\r\nstandalone\r\n"
          + "$4\r\nrole\r\n$6\r\nmaster\r\n" + "$7\r\nmodules\r\n*0\r\n");

    responses.put(new CommandKey(Protocol.Command.PING), "+PONG\r\n");

    // CLIENT subcommands
    responses.put(new CommandKey(Protocol.Command.CLIENT, Protocol.Keyword.SETNAME), "+OK\r\n");
    responses.put(new CommandKey(Protocol.Command.CLIENT, Protocol.Keyword.SETINFO), "+OK\r\n");
    responses.put(new CommandKey("CLIENT", "TRACKING"), "+OK\r\n");

    BUILTIN_RESPONSES = java.util.Collections.unmodifiableMap(responses);
  }

  /**
   * Simple implementation of ProtocolCommand for unknown commands.
   */
  private static class SimpleProtocolCommand implements ProtocolCommand {
    private final byte[] raw;
    private final int hashCode;

    public SimpleProtocolCommand(String command) {
      this.raw = SafeEncoder.encode(command);
      this.hashCode = Arrays.hashCode(raw);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ProtocolCommand)) return false;
      ProtocolCommand that = (ProtocolCommand) o;
      return Arrays.equals(raw, that.getRaw());
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      return SafeEncoder.encode(raw);
    }
  }

  /**
   * Key for command lookup in the registry. Supports command + optional subcommand.
   */
  private static class CommandKey {
    private final String command;
    private final String subcommand;
    private final int hashCode;

    public CommandKey(ProtocolCommand command) {
      this(command, null);
    }

    public CommandKey(ProtocolCommand command, redis.clients.jedis.args.Rawable subcommand) {
      this.command = SafeEncoder.encode(command.getRaw()).toUpperCase();
      this.subcommand = subcommand != null ? SafeEncoder.encode(subcommand.getRaw()).toUpperCase()
          : null;
      this.hashCode = java.util.Objects.hash(this.command, this.subcommand);
    }

    public CommandKey(String command, String subcommand) {
      this.command = command.toUpperCase();
      this.subcommand = subcommand != null ? subcommand.toUpperCase() : null;
      this.hashCode = java.util.Objects.hash(this.command, this.subcommand);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CommandKey)) return false;
      CommandKey that = (CommandKey) o;
      return command.equals(that.command) && java.util.Objects.equals(subcommand, that.subcommand);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      return subcommand != null ? command + " " + subcommand : command;
    }
  }

  /**
   * Client handler for each connection
   */
  private class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String clientId;
    private RedisOutputStream outputStream;
    private volatile boolean connected = true;
    private final Object outputLock = new Object(); // Lock to prevent interleaving

    public ClientHandler(Socket clientSocket) {
      this.clientSocket = clientSocket;
      this.clientId = clientSocket.getRemoteSocketAddress().toString();
    }

    @Override
    public void run() {
      try (RedisInputStream rin = new RedisInputStream(clientSocket.getInputStream());
          RedisOutputStream out = new RedisOutputStream(clientSocket.getOutputStream())) {

        this.outputStream = out;
        connectedClients.put(clientId, this);

        Object input;
        while (connected && !clientSocket.isClosed()) {
          try {
            input = Protocol.read(rin);
            if (input == null) {
              connected = false;
              break;
            }

            // Deserialize into CommandArguments
            List<byte[]> rawArgs = (List<byte[]>) input;
            CommandArguments commandArgs = deserializeToCommandArguments(rawArgs);

            // Process command with custom handler or built-in responses
            processCommand(commandArgs);
          } catch (IOException e) {
            logger.debug("Client " + clientId + " disconnected: " + e.getMessage());
            connected = false;
            break;
          } catch (Exception e) {
            logger.debug("Client " + clientId + " connection error: " + e.getMessage());
            connected = false;
            break;
          }
        }
      } catch (IOException e) {
        logger.error("Error handling client: " + e.getMessage());
      } finally {
        cleanup();
      }
    }

    /**
     * Deserialize raw byte arrays into CommandArguments. First element is the command, rest are
     * arguments.
     */
    private CommandArguments deserializeToCommandArguments(List<byte[]> rawArgs) {
      if (rawArgs == null || rawArgs.isEmpty()) {
        throw new IllegalArgumentException("Empty command");
      }

      // First element is the command - try to match it to a known ProtocolCommand
      String cmdString = SafeEncoder.encode(rawArgs.get(0)).toUpperCase();
      ProtocolCommand command = findProtocolCommand(cmdString);

      // If no known command found, create a simple wrapper
      if (command == null) {
        command = new SimpleProtocolCommand(cmdString);
      }

      // Create CommandArguments with the command
      CommandArguments commandArgs = new CommandArguments(command);

      // Add remaining arguments
      for (int i = 1; i < rawArgs.size(); i++) {
        commandArgs.add(rawArgs.get(i));
      }

      return commandArgs;
    }

    /**
     * Try to find a matching ProtocolCommand from Protocol.Command enum.
     */
    private ProtocolCommand findProtocolCommand(String cmdString) {
      try {
        return Protocol.Command.valueOf(cmdString);
      } catch (IllegalArgumentException e) {
        // Not a standard command, return null
        return null;
      }
    }

    /**
     * Process a command by first checking if a custom command handler is available, otherwise
     * falling back to predefined built-in responses.
     * @param commandArgs the command arguments
     * @throws IOException if writing the response fails
     */
    private void processCommand(CommandArguments commandArgs) throws IOException {
      String response = null;

      // First, try custom command handler if available
      if (commandHandler != null) {
        response = commandHandler.handleCommand(commandArgs, clientId);
      }

      // If no custom handler or it returned null, fall back to built-in responses
      if (response == null) {
        response = getBuiltinResponse(commandArgs);
      }

      // Write the response
      if (response != null) {
        writeResponse(response);
      } else {
        throw new RuntimeException("Unknown command: " + commandArgs.getCommand());
      }
    }

    /**
     * Synchronized method to write response to output stream. This ensures thread-safe access to
     * the non-thread-safe RedisOutputStream.
     */
    private void writeResponse(String response) throws IOException {
      synchronized (outputLock) {
        if (outputStream != null && connected) {
          outputStream.write(response.getBytes());
          outputStream.flush();
        }
      }
    }

    /**
     * Get the built-in response for a command from the response registry. Uses smart lookup: tries
     * command + first argument first, then just command.
     * @param commandArgs the command arguments
     * @return the response string, or null if no built-in response exists
     */
    private String getBuiltinResponse(CommandArguments commandArgs) {
      String cmdString = SafeEncoder.encode(commandArgs.getCommand().getRaw());
      String subcommand = null;

      // Extract subcommand if present (first argument)
      if (commandArgs.size() > 1) {
        subcommand = SafeEncoder.encode(commandArgs.get(1).getRaw());
      }

      // Try lookup with command + subcommand first (e.g., "CLIENT SETNAME")
      if (subcommand != null) {
        CommandKey key = new CommandKey(cmdString, subcommand);
        String response = BUILTIN_RESPONSES.get(key);
        if (response != null) {
          return response;
        }
      }

      // Fall back to command only (e.g., "CLIENT")
      CommandKey key = new CommandKey(cmdString, null);
      return BUILTIN_RESPONSES.get(key);
    }

    /**
     * Clean up client resources and remove from connected clients map
     */
    private void cleanup() {
      connected = false;
      connectedClients.remove(clientId);

      // Synchronize to ensure no push message is being sent while we clean up
      synchronized (outputLock) {
        outputStream = null;
      }

      try {
        if (clientSocket != null && !clientSocket.isClosed()) {
          clientSocket.close();
        }
      } catch (IOException e) {
        logger.error("Error closing client socket during cleanup: " + e.getMessage());
      }
    }

    /**
     * Generic method to send a push message to this client. According to RESP3 spec, push messages
     * may precede or follow command replies, but must not interleave with them. We use
     * synchronization to ensure this.
     * @param pushType the type of push message (e.g., "MIGRATING", "MIGRATED")
     * @param args optional arguments for the push message
     */
    public void sendPushMessage(String pushType, String... args) {
      try {
        StringBuilder pushMessage = new StringBuilder();

        // Calculate total number of elements (push type + arguments)
        int elementCount = 1 + args.length;
        pushMessage.append(">").append(elementCount).append("\r\n");

        // Add push type
        pushMessage.append("$").append(pushType.length()).append("\r\n").append(pushType)
            .append("\r\n");

        // Add arguments
        for (String arg : args) {
          pushMessage.append("$").append(arg.length()).append("\r\n").append(arg).append("\r\n");
        }

        // Use synchronized writeResponse method to prevent interleaving
        writeResponse(pushMessage.toString());

      } catch (IOException e) {
        logger.error("Error sending " + pushType + " push to " + clientId
            + " (client disconnected): " + e.getMessage());
        cleanup();
      }
    }

    /**
     * Force close this client connection (used when server is shutting down)
     */
    public void forceClose() {
      connected = false;

      try {
        if (clientSocket != null && !clientSocket.isClosed()) {
          clientSocket.close();
        }
      } catch (IOException e) {
        logger.error("Error force closing client socket: " + e.getMessage());
      }

      // Remove from connected clients map
      connectedClients.remove(clientId);
      outputStream = null;
    }

  }

}
