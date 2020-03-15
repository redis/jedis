package redis.clients.jedis;

import java.io.Serializable;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostAndPort implements Serializable {
  private static final long serialVersionUID = -519876229978427751L;

  protected static Logger log = LoggerFactory.getLogger(HostAndPort.class.getName());
  public static volatile String localhost;


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
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof HostAndPort)) return false;

    HostAndPort hp = (HostAndPort) obj;

    String thisHost = convertHost(host);
    String hpHost = convertHost(hp.host);
    return port == hp.port && thisHost.equals(hpHost);
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
    int idx     = from.lastIndexOf(':');
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
      int port = Integer.parseInt(parts[1]);
      return new HostAndPort(convertHost(host), port);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  public static String convertHost(String host) {
    try {
        /*
         * Validate the host name as an IPV4/IPV6 address.
         * If this is an AWS ENDPOINT it will not parse.
         * In that case accept host as is.
         *
         * Costs: If this is an IPV4/6 encoding, e.g. 127.0.0.1 then no DNS lookup
         * is done.  If it is a name then a DNS lookup is done but it is normally cached.
         * Secondarily, this class is typically used to create a connection once
         * at the beginning of processing and then not used again.  So even if the DNS
         * lookup needs to be done then the cost is miniscule.
         */
      InetAddress inetAddress = InetAddress.getByName(host);

      // isLoopbackAddress() handles both IPV4 and IPV6
      if (inetAddress.isLoopbackAddress() || host.equals("0.0.0.0") || host.startsWith("169.254"))
        return getLocalhost();
      else
        return host;
    } catch (Exception e) {
      // Not a valid IP address
      log.warn("{}.convertHost '" + host + "' is not a valid IP address. ", HostAndPort.class.getName(), e);
      return host;
    }
  }

  public static void setLocalhost(String localhost) {
    synchronized (HostAndPort.class) {
      HostAndPort.localhost = localhost;
    }
  }

  /**
   * This method resolves the localhost in a 'lazy manner'.
   *
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
    } catch (Exception ex) {
      log.error("{}.getLocalHostQuietly : cant resolve localhost address",
        HostAndPort.class.getName(), ex);
      localAddress = "localhost";
    }
    return localAddress;
  }
}
