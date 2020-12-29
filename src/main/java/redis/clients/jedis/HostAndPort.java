package redis.clients.jedis;

import java.io.Serializable;

public class HostAndPort implements Serializable {
  private static final long serialVersionUID = -519876229978427751L;

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

    HostAndPort hp = (HostAndPort) obj;
    return port == hp.port && host.equals(hp.host);
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
   *
   * @param string String to parse. Must be in <b>"host:port"</b> format. Port is mandatory.
   * @return parsed HostAndPort
   */
  public static HostAndPort from(String string) {
    int lastColon = string.lastIndexOf(":");
    String host = string.substring(0, lastColon);
    int port = Integer.parseInt(string.substring(lastColon + 1));
    return new HostAndPort(host, port);
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
      return new HostAndPort(host, port);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
