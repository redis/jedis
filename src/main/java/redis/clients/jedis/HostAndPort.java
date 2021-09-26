package redis.clients.jedis;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostAndPort implements Serializable {

  private static final long serialVersionUID = -519876229978427751L;

  protected static Logger log = LoggerFactory.getLogger(HostAndPort.class);

  private static volatile String localhost;

  private final String host;
  private final int port;

  public HostAndPort(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof HostAndPort)) return false;

    HostAndPort other = (HostAndPort) obj;

    return this.port == other.port && this.host.equals(other.host);
  }

  @Override
  public int hashCode() {
    return 31 * host.hashCode() + port;
  }

  @Override
  public String toString() {
    return host + ":" + port;
  }

  /**
   * Creates HostAndPort with <i>unconverted</i> host.
   * @param string String to parse. Must be in <b>"host:port"</b> format. Port is mandatory.
   * @return parsed HostAndPort
   */
  public static HostAndPort from(String string) {
    int lastColon = string.lastIndexOf(":");
    String host = string.substring(0, lastColon);
    int port = Integer.parseInt(string.substring(lastColon + 1));
    return new HostAndPort(host, port);
  }

  public static void setLocalhost(String localhost) {
    synchronized (HostAndPort.class) {
      HostAndPort.localhost = localhost;
    }
  }

  /**
   * This method resolves the localhost in a 'lazy manner'.
   * @return localhost
   */
  public static String getLocalhost() {
    if (localhost == null) {
      synchronized (HostAndPort.class) {
        if (localhost == null) {
          return localhost = getLocalHostQuietly();
        }
      }
    }
    return localhost;
  }

  public static String getLocalHostQuietly() {
    String localAddress;
    try {
      localAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException | RuntimeException ex) {
      log.error("{}.getLocalHostQuietly : cant resolve localhost address",
        HostAndPort.class.getName(), ex);
      localAddress = "localhost";
    }
    return localAddress;
  }
}
