package redis.clients.jedis;

import java.net.SocketAddress;

/**
 * Maps a resolved peer address to the address the socket should actually connect to. Applied by a
 * {@link JedisSocketFactory} after address resolution and before
 * {@link java.net.Socket#connect(SocketAddress, int)}, so the input is typically an
 * {@link java.net.InetSocketAddress}; the type is intentionally {@link SocketAddress} so non-TCP
 * factories (e.g. a Unix domain socket factory) can use the same seam.
 */
@FunctionalInterface
interface SocketAddressMapper {

  /**
   * @param resolved the post-resolve address the socket would otherwise connect to
   * @return the address to connect to instead, or {@code null} to leave {@code resolved} unchanged
   */
  SocketAddress getSocketAddress(SocketAddress resolved);
}
