package redis.clients.jedis;

import java.io.IOException;
import java.net.Socket;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * JedisSocketFactory: responsible for creating socket connections from the within the Jedis client,
 * the default socket factory will create TCP sockets with the recommended configuration.
 * <p>
 * You can use a custom JedisSocketFactory for many use cases, such as: - a custom address resolver
 * - a unix domain socket - a custom configuration for you TCP sockets
 */
public interface JedisSocketFactory {

  /**
   * WARNING: Throwing IOException will not be supported in future.
   * @return Socket
   * @throws IOException this will be removed in future
   * @throws JedisConnectionException
   */
  Socket createSocket() throws IOException, JedisConnectionException;

  void updateHostAndPort(HostAndPort hostAndPort);

  @Deprecated
  String getDescription();

  @Deprecated
  String getHost();

  @Deprecated
  void setHost(String host);

  @Deprecated
  int getPort();

  @Deprecated
  void setPort(int port);

  @Deprecated
  int getConnectionTimeout();

  @Deprecated
  void setConnectionTimeout(int connectionTimeout);

  @Deprecated
  int getSoTimeout();

  @Deprecated
  void setSoTimeout(int soTimeout);
}
