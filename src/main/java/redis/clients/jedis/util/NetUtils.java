package redis.clients.jedis.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class NetUtils {

  private NetUtils() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Determine if the given {@link SocketAddress} represents a private IP address: loopback,
   * link-local, site-local, or IPv6 unique-local.
   * @param socketAddress the address to check
   * @return {@code true} if the address is a resolved private IP address
   * @since 8.0
   */
  public static boolean isPrivateIp(SocketAddress socketAddress) {
    if (!(socketAddress instanceof InetSocketAddress)) {
      return false;
    }

    InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
    if (address == null || address.isAnyLocalAddress()) {
      return false;
    }

    return address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || isUniqueLocalAddress(address);
  }

  // https://datatracker.ietf.org/doc/html/rfc4193
  private static boolean isUniqueLocalAddress(InetAddress address) {
    if (!(address instanceof Inet6Address)) {
      return false;
    }
    byte[] bytes = address.getAddress();
    return (bytes[0] & (byte) 0xfe) == (byte) 0xfc; // fc00::/7
  }
}