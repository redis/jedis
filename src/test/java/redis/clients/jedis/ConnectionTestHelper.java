package redis.clients.jedis;

import redis.clients.jedis.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Helper class for testing Connection objects, providing access to internal state
 * that is not normally exposed through the public API.
 */
public class ConnectionTestHelper {

  /**
   * Get the actual HostAndPort from the underlying socket connection.
   * This extracts the real remote address that the socket is connected to,
   * which may differ from the configured address in cases of rebinding.
   *
   * @param connection the Connection to extract the address from
   * @return the actual HostAndPort of the remote socket connection
   * @throws RuntimeException if unable to access the socket or extract the address
   */
  public static HostAndPort getHostAndPort(Connection connection) {
      Socket socket = ReflectionTestUtils.getField(connection, "socket");

      SocketAddress remoteAddress = socket.getRemoteSocketAddress();

      if (remoteAddress instanceof InetSocketAddress) {
        InetSocketAddress inetAddress = (InetSocketAddress) remoteAddress;
        String host = inetAddress.getHostString();
        int port = inetAddress.getPort();
        return new HostAndPort(host, port);
      } else {
        throw new RuntimeException("Unexpected socket address type: " + remoteAddress.getClass());
      }
  }
}
