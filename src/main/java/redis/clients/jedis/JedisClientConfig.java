package redis.clients.jedis;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public interface JedisClientConfig {

  default RedisProtocol getRedisProtocol() {
    return null;
  }

  /**
   * @return Connection timeout in milliseconds
   */
  default int getConnectionTimeoutMillis() {
    return Protocol.DEFAULT_TIMEOUT;
  }

  /**
   * @return Socket timeout in milliseconds
   */
  default int getSocketTimeoutMillis() {
    return Protocol.DEFAULT_TIMEOUT;
  }

  /**
   * @return Socket timeout (in milliseconds) to use during blocking operation. Default is '0',
   * which means to block forever.
   */
  default int getBlockingSocketTimeoutMillis() {
    return 0;
  }

  /**
   * @return Redis ACL user
   */
  default String getUser() {
    return null;
  }

  default String getPassword() {
    return null;
  }

  default Supplier<RedisCredentials> getCredentialsProvider() {
    return new DefaultRedisCredentialsProvider(
        new DefaultRedisCredentials(getUser(), getPassword()));
  }

  default int getDatabase() {
    return Protocol.DEFAULT_DATABASE;
  }

  default String getClientName() {
    return null;
  }

  /**
   * @return {@code true} - to create TLS connection(s). {@code false} - otherwise.
   */
  default boolean isSsl() {
    return false;
  }

  default SSLSocketFactory getSslSocketFactory() {
    return null;
  }

  default SSLParameters getSslParameters() {
    return null;
  }

  default HostnameVerifier getHostnameVerifier() {
    return null;
  }

  default HostAndPortMapper getHostAndPortMapper() {
    return null;
  }

  /**
   * Execute READONLY command to connections.
   * <p>
   * READONLY command is specific to Redis Cluster replica nodes. So this config param is only
   * intended for Redis Cluster connections.
   * @return {@code true} - to execute READONLY command to connection(s). {@code false} - otherwise.
   */
  default boolean isReadOnlyForRedisClusterReplicas() {
    return false;
  }

  /**
   * Modify the behavior of internally executing CLIENT SETINFO command.
   * @return CLIENT SETINFO config
   */
  default ClientSetInfoConfig getClientSetInfoConfig() {
    return ClientSetInfoConfig.DEFAULT;
  }
}
