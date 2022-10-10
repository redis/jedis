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
}
