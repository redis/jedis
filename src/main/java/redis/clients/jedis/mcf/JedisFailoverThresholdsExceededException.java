package redis.clients.jedis.mcf;

import redis.clients.jedis.exceptions.JedisException;

public class JedisFailoverThresholdsExceededException extends JedisException {
  public JedisFailoverThresholdsExceededException(String s, Exception e) {
    super(s, e);
  }

  public JedisFailoverThresholdsExceededException(String s) {
    super(s);
  }
}
