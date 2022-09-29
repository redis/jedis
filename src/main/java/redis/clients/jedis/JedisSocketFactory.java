package redis.clients.jedis;

import java.net.Socket;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * JedisSocketFactory: responsible for creating socket connections
 * from the within the Jedis client, the default socket factory will
 * create TCP sockets with the recommended configuration.
 * <p>
 * You can use a custom JedisSocketFactory for many use cases, such as:
 * - a custom address resolver
 * - a unix domain socket
 * - a custom configuration for you TCP sockets
 */
public interface JedisSocketFactory {

  Socket createSocket() throws JedisConnectionException;
}
