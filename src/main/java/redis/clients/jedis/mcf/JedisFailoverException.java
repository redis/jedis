package redis.clients.jedis.mcf;

import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisFailoverException extends JedisConnectionException {
  private static final String MESSAGE = "Cluster/database endpoint could not failover since the MultiClusterClientConfig was not "
      + "provided with an additional cluster/database endpoint according to its prioritized sequence. "
      + "If applicable, consider falling back OR restarting with an available cluster/database endpoint";

  public JedisFailoverException(String s) {
    super(s);
  }

  public JedisFailoverException() {
    super(MESSAGE);
  }

  public static class JedisPermanentlyNotAvailableException extends JedisFailoverException {
    public JedisPermanentlyNotAvailableException(String s) {
      super(s);
    }

    public JedisPermanentlyNotAvailableException() {
      super();
    }
  }

  public static class JedisTemporarilyNotAvailableException extends JedisFailoverException {

    public JedisTemporarilyNotAvailableException(String s) {
      super(s);
    }

    public JedisTemporarilyNotAvailableException() {
      super();
    }
  }
}