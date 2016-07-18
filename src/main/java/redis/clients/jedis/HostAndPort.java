package redis.clients.jedis;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HostAndPort implements Serializable {
  private static final long serialVersionUID = -519876229978427751L;

  protected static Logger log = Logger.getLogger(HostAndPort.class.getName());
  public static final String LOCALHOST_STR = getLocalHostQuietly();


  private String host;
  private int port;

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
    if (obj instanceof HostAndPort) {
      HostAndPort hp = (HostAndPort) obj;

      String thisHost = convertHost(host);
      String hpHost = convertHost(hp.host);
      return port == hp.port && thisHost.equals(hpHost);

    }

    return false;
  }

  @Override
  public int hashCode() {
    return 31 * convertHost(host).hashCode() + port;
  }

  @Override
  public String toString() {
    return host + ":" + port;
  }

  /**
   * Splits String into host and port parts.
   * String must be in ( host + ":" + port ) format.
   * Port is optional
   * @param from String to parse
   * @return array of host and port strings
     */
  public static String[] extractParts(String from){
    int idx     = from.lastIndexOf(":");
    String host = idx != -1 ? from.substring(0, idx)  : from;
    String port = idx != -1 ? from.substring(idx + 1) : "";
    return new String[] { host, port };
  }

  /**
   * Creates HostAndPort instance from string.
   * String must be in ( host + ":" + port ) format.
   * Port is mandatory. Can convert host part.
   * @see #convertHost(String)
   * @param from String to parse
   * @return HostAndPort instance
     */
  public static HostAndPort parseString(String from){
    // NOTE: redis answers with
    // '99aa9999aa9a99aa099aaa990aa99a09aa9a9999 9a09:9a9:a090:9a::99a slave 8c88888888cc08088cc8c8c888c88c8888c88cc8 0 1468251272993 37 connected'
    // for CLUSTER NODES, ASK and MOVED scenarios. That's why there is no possibility to parse address in 'correct' way.
    // Redis should switch to 'bracketized' (RFC 3986) IPv6 address.
    try {
      String[] parts = extractParts(from);
      String host = parts[0];
      int port = Integer.valueOf(parts[1]);
      return new HostAndPort(convertHost(host), port);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  public static String convertHost(String host) {
    if (host.equals("127.0.0.1") || host.startsWith("localhost") || host.equals("0.0.0.0") ||
        host.startsWith("169.254") ||
        host.startsWith("::1") || host.startsWith("0:0:0:0:0:0:0:1")) {
      return LOCALHOST_STR;
    } else {
      return host;
    }
  }

  public static String getLocalHostQuietly() {
    String localAddress;
    try {
      localAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (Exception ex) {
      log.logp(Level.SEVERE, HostAndPort.class.getName(), "getLocalHostQuietly", "cant resolve localhost address", ex);
      localAddress = "localhost";
    }
    return localAddress;
  }
}
