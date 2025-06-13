package redis.clients.jedis.mcf;

import redis.clients.jedis.exceptions.JedisException;

public class ConnectionFailoverException extends JedisException {
  public ConnectionFailoverException(String s, Exception e) {
    super(s, e);
  }
}
