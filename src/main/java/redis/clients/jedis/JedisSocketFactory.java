package redis.clients.jedis;

import java.io.IOException;
import java.net.Socket;

/**
 * JedisSocketFactory: responsible for creating socket connections
 * from the within the Jedis client, the default socket factory will
 * create TCP sockets with the recommended configuration.
 *
 * You can use a custom JedisSocketFactory for many use cases, such as:
 * - a custom address resolver
 * - a unix domain socket
 * - a custom configuration for you TCP sockets
 */
public interface JedisSocketFactory {

  Socket createSocket() throws IOException;

  String getDescription();

  String getHost();

  void setHost(String host);

  int getPort();

  void setPort(int port);

  int getConnectionTimeout();

  void setConnectionTimeout(int connectionTimeout);

  int getSoTimeout();

  void setSoTimeout(int soTimeout);
}
