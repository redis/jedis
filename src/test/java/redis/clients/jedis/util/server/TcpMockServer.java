package redis.clients.jedis.util.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
   * @param args optional arguments for the push message (can be String or Long)
   */
  public void sendPushMessageToAll(String pushType, Object... args) {
    connectedClients.values().forEach(client -> client.sendPushMessage(pushType, args));
  }

  /**
   * Send a MIGRATING push message to all connected clients
   */
  public void sendMigratingPushToAll() {
    sendPushMessageToAll("MIGRATING", 30);
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
    sendPushMessageToAll("FAILING_OVER", 30);
  }

  /**
   * Send a FAILED_OVER push message to all connected clients
   */
  public void sendFailedOverPushToAll() {
    sendPushMessageToAll("FAILED_OVER");
  }

  /**
   * Send a MOVING push message according to RESP3 spec to all connected clients
   * Format: >4\r\n+MOVING\r\n:<seq_number>\r\n:<time_s>\r\n+<new_name_or_ip>:<port>\r\n
   * @param expiry the timestamp in seconds as Long
   * @param targetHost the target host:port
   */
  public void sendMovingPushToAll( Long expiry, String targetHost) {
    sendPushMessageToAll("MOVING", expiry, targetHost);
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
   * Client handler for each connection
   */
  private class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String clientId;
    private RedisOutputStream outputStream;
    private volatile boolean connected = true;

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

            List<Object> cmdArgs = (List<Object>) input;
            String cmdString = SafeEncoder.encode((byte[]) cmdArgs.get(0));

            // Convert arguments to strings (excluding command name)
            List<String> args = new java.util.ArrayList<>();
            for (int i = 1; i < cmdArgs.size(); i++) {
              args.add(SafeEncoder.encode((byte[]) cmdArgs.get(i)));
            }

            // Try custom handler first
            String customResponse = null;
            if (commandHandler != null) {
              customResponse = commandHandler.handleCommand(cmdString, args, clientId);
            }

            if (customResponse != null) {
              out.write(customResponse.getBytes());
              out.flush();
            } else {
              // Handle with default built-in handlers
              handleBuiltinCommand(cmdString, out);
            }
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

    private void sendHelloResponse(OutputStream out) throws IOException {
      // RESP3 HELLO response
      String response = "%7\r\n" + "$6\r\nserver\r\n$5\r\nredis\r\n"
          + "$7\r\nversion\r\n$5\r\n7.0.0\r\n" + "$5\r\nproto\r\n:3\r\n" + "$2\r\nid\r\n:1\r\n"
          + "$4\r\nmode\r\n$10\r\nstandalone\r\n" + "$4\r\nrole\r\n$6\r\nmaster\r\n"
          + "$7\r\nmodules\r\n*0\r\n";
      out.write(response.getBytes());
      out.flush();
    }

    private void sendPongResponse(OutputStream out) throws IOException {
      String response = "+PONG\r\n";
      out.write(response.getBytes());
      out.flush();
    }

    private void sendOkResponse(OutputStream out) throws IOException {
      String response = "+OK\r\n";
      out.write(response.getBytes());
      out.flush();
    }

    /**
     * Handle a command with built-in handlers.
     */
    private void handleBuiltinCommand(String cmdString, OutputStream out) throws IOException {
      if (cmdString.equalsIgnoreCase("HELLO")) {
        sendHelloResponse(out);
      } else if (cmdString.contains("PING")) {
        sendPongResponse(out);
      } else if (cmdString.contains("CLIENT")) {
        sendOkResponse(out);
      } else {
        throw new RuntimeException("Unknown command: " + cmdString);
      }
    }

    /**
     * Clean up client resources and remove from connected clients map
     */
    private void cleanup() {
      connected = false;
      connectedClients.remove(clientId);
      outputStream = null;

      try {
        if (clientSocket != null && !clientSocket.isClosed()) {
          clientSocket.close();
        }
      } catch (IOException e) {
        logger.error("Error closing client socket during cleanup: " + e.getMessage());
      }
    }

    /**
     * Generic method to send a push message to this client.
     * @param pushType the type of push message (e.g., "MIGRATING", "MIGRATED")
     * @param args optional arguments for the push message (can be String or Long)
     */
    public void sendPushMessage(String pushType, Object... args) {
      try {
        StringBuilder pushMessage = new StringBuilder();

        // Calculate total number of elements (push type + arguments)
        int elementCount = 1 + args.length;
        pushMessage.append(">").append(elementCount).append("\r\n");

        // Add push type as simple string (RESP3 spec)
        pushMessage.append("+").append(pushType).append("\r\n");

        // Add arguments
        for (Object arg : args) {
          if (arg instanceof Long || arg instanceof Integer) {
            // Format numeric values as Redis integer
            pushMessage.append(":").append(arg).append("\r\n");
          } else if (arg instanceof String) {
            // Format String as Redis simple string (RESP3 spec for push notifications)
            String strArg = (String) arg;
            pushMessage.append("+").append(strArg).append("\r\n");
          } else {
            // Convert other types to string and format as simple string
            String strArg = arg.toString();
            pushMessage.append("+").append(strArg).append("\r\n");
          }
        }

        outputStream.write(pushMessage.toString().getBytes());
        outputStream.flush();

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
