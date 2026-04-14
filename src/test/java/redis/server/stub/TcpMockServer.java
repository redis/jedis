package redis.server.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.CommandArguments;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A simple TCP mock server for testing Redis push notifications and timeout behavior. This server
 * can accept connections and send predefined responses including push messages.
 */
abstract class TcpMockServer {
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Map<Long, ClientHandler> clients = new ConcurrentHashMap<>();
  Logger logger = LoggerFactory.getLogger(TcpMockServer.class);
  private ServerSocket serverSocket;
  private int port;

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
          ClientHandler clientHandler = new ClientHandler(clientSocket, this);
          clients.put(clientHandler.getClientState().getId(), clientHandler);
          executor.submit(() -> {
            try {
              clientHandler.run();
            } finally {
              clients.remove(clientHandler.getClientState().getId(), clientHandler);
            }
          });
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
   * Process a command and return the RESP-formatted response.
   * @param args command arguments
   * @param clientState client state
   * @param clientHandler client handler (for pub/sub push messages)
   * @return RESP-formatted response
   */
  abstract String processCommand(CommandArguments args, ClientState clientState,
      ClientHandler clientHandler);

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
    return clients.size();
  }

  /**
   * Generic method to send a push message to all connected clients.
   * @param args optional arguments for the push message
   */
  public void sendPushMessageToAll(String... args) {
    onEachClient(client -> client.sendPushMessage(args));
  }

  /**
   * Send a push message to a specific client by ID.
   * @param clientId the client ID
   * @param args optional arguments for the push message
   * @return true if the client was found and message sent, false otherwise
   */
  public boolean sendPushMessageToClient(long clientId, String... args) {
    ClientHandler client = clients.get(clientId);
    if (client != null) {
      client.sendPushMessage(args);
      return true;
    }
    return false;
  }

  protected void onEachClient(Consumer<ClientHandler> action) {
    clients.values().forEach(action);
  }

  /**
   * Close all active client connections
   */
  private void closeAllActiveConnections() {
    // Create a copy of the values to avoid ConcurrentModificationException
    List<ClientHandler> clientsToClose = new ArrayList<>(clients.values());

    for (ClientHandler client : clientsToClose) {
      try {
        client.forceClose();
      } catch (Exception e) {
        logger.error("Error closing client connection: " + e.getMessage());
      }
    }

    // Clear the map
    clients.clear();
  }
}
