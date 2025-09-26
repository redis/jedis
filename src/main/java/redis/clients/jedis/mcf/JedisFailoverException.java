package redis.clients.jedis.mcf;

import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Exception thrown when a failover attempt fails due to lack of available/healthy clusters.
 * <p>
 * This exception itself is not thrown, see the child exceptions for more details.
 * </p>
 * @see JedisFailoverException.JedisPermanentlyNotAvailableException
 * @see JedisFailoverException.JedisTemporarilyNotAvailableException
 */
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

  /**
   * Exception thrown when a failover attempt fails due to lack of available/healthy clusters, and
   * the max number of failover attempts has been exceeded. And there is still no healthy cluster.
   * See the configuration properties
   * {@link redis.clients.jedis.MultiClusterClientConfig#maxNumFailoverAttempts} and
   * {@link redis.clients.jedis.MultiClusterClientConfig#delayInBetweenFailoverAttempts} for more
   * details.
   */
  public static class JedisPermanentlyNotAvailableException extends JedisFailoverException {
    public JedisPermanentlyNotAvailableException(String s) {
      super(s);
    }

    public JedisPermanentlyNotAvailableException() {
      super();
    }
  }

  /**
   * Exception thrown when a failover attempt fails due to lack of available/healthy clusters, but
   * the max number of failover attempts has not been exceeded yet. Though there is no healthy
   * cluster including the selected/current one, given configuration suggests that it should be a
   * temporary condition and it is possible that there will be a healthy cluster available.
   * See the configuration properties
   * {@link redis.clients.jedis.MultiClusterClientConfig#maxNumFailoverAttempts} and
   * {@link redis.clients.jedis.MultiClusterClientConfig#delayInBetweenFailoverAttempts} for more
   * details.
   */
  public static class JedisTemporarilyNotAvailableException extends JedisFailoverException {

    public JedisTemporarilyNotAvailableException(String s) {
      super(s);
    }

    public JedisTemporarilyNotAvailableException() {
      super();
    }
  }
}