package redis.server.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Handles a single client connection to TcpMockServer. Responsible for reading commands from the
 * client socket, processing them via the CommandProcessor, and writing responses back.
 */
class ClientHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

  private final Socket clientSocket;
  private final ClientState clientState;
  private final TcpMockServer server;
  private RedisOutputStream outputStream;
  private volatile boolean connected = true;
  private final Object outputLock = new Object(); // Lock to prevent interleaving

  public ClientHandler(Socket clientSocket, TcpMockServer server) {
    this.clientSocket = clientSocket;
    this.clientState = new ClientState();
    this.server = server;
  }

  /**
   * Get the client ID for this handler.
   * @return client ID
   */
  public ClientState getClientState() {
    return clientState;
  }

  @Override
  public void run() {
    try (RedisInputStream rin = new RedisInputStream(clientSocket.getInputStream());
        RedisOutputStream out = new RedisOutputStream(clientSocket.getOutputStream())) {

      this.outputStream = out;

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
          logger.debug("Client " + clientState.getId() + " disconnected: " + e.getMessage());
          connected = false;
          break;
        } catch (Exception e) {
          logger.debug("Client " + clientState.getId() + " connection error: " + e.getMessage());
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
   * Simple implementation of ProtocolCommand for unknown commands.
   */
  private static class SimpleProtocolCommand implements ProtocolCommand {
    private final byte[] raw;

    public SimpleProtocolCommand(String command) {
      this.raw = SafeEncoder.encode(command);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  /**
   * Process a command using the command processor.
   * @param commandArgs the command arguments
   * @throws IOException if writing the response fails
   */
  private void processCommand(CommandArguments commandArgs) throws IOException {
    // Use the command processor (customizable by subclasses)
    String response = server.processCommand(commandArgs, clientState);

    if (response != null) {
      writeResponse(response);
    }
  }

  /**
   * Synchronized method to write response to output stream. This ensures thread-safe access to the
   * non-thread-safe RedisOutputStream.
   */
  protected void writeResponse(String response) throws IOException {
    synchronized (outputLock) {
      if (outputStream != null && connected) {
        outputStream.write(response.getBytes());
        outputStream.flush();
      }
    }
  }

  /**
   * Clean up client resources and remove from connected clients map
   */
  private void cleanup() {
    connected = false;

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
   * may precede or follow command replies, but must not interleave with them.
   * @param args push message elements (first is message type)
   */
  public void sendPushMessage(String... args) {
    try {
      // Use RespResponse.push() to build proper RESP3 push message
      String pushMessage = RespResponse.push(args);
      writeResponse(pushMessage);

    } catch (IOException e) {
      logger.error("Error sending " + args[0] + " push to " + clientState.getId()
          + " (client disconnected): " + e.getMessage());
      cleanup();
    }
  }

  /**
   * Force close this client connection (used when server is shutting down)
   */
  public void forceClose() {
    connected = false;

    synchronized (outputLock) {
      try {
        if (clientSocket != null && !clientSocket.isClosed()) {
          clientSocket.close();
        }
      } catch (IOException e) {
        logger.error("Error force closing client socket: " + e.getMessage());
      }
      outputStream = null;
    }

  }

}
