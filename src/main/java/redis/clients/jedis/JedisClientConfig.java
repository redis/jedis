package redis.clients.jedis;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.authentication.AuthXManager;

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
   *         which means to block forever.
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

  // TODO: return null
  default Supplier<RedisCredentials> getCredentialsProvider() {
    return new DefaultRedisCredentialsProvider(
        new DefaultRedisCredentials(getUser(), getPassword()));
  }

  default AuthXManager getAuthXManager() {
    return null;
  }

  default int getDatabase() {
    return Protocol.DEFAULT_DATABASE;
  }

  default String getClientName() {
    return null;
  }

  /**
   * Enable TLS/SSL for connections.
   * <p>
   * <b>Prefer {@link #getSslOptions()}</b> for simpler configuration with built-in hostname
   * verification and certificate validation modes.
   * <p>
   * When {@code true} without custom {@link #getSslParameters()}, hostname verification is enabled
   * by default. Custom {@link SSLParameters} can override this behavior.
   * @return {@code true} - to create TLS connection(s). {@code false} - otherwise.
   * @see #getSslOptions()
   */
  default boolean isSsl() {
    return false;
  }

  /**
   * Custom {@link SSLSocketFactory} for TLS connections.
   * <p>
   * <b>Prefer {@link #getSslOptions()}</b> for simpler configuration.
   * @return Custom SSL socket factory, or {@code null} to use default
   * @see #getSslOptions()
   */
  default SSLSocketFactory getSslSocketFactory() {
    return null;
  }

  /**
   * Custom {@link SSLParameters} for TLS connections.
   * <p>
   * <b>Prefer {@link #getSslOptions()}</b> for simpler configuration with built-in hostname
   * verification modes.
   * @return Custom SSL parameters, or {@code null} to use defaults with hostname verification
   * @see #getSslOptions()
   */
  default SSLParameters getSslParameters() {
    return null;
  }

  /**
   * Recommended way to configure TLS/SSL connections.
   * <p>
   * Provides simple builder API with built-in support for certificate validation modes, hostname
   * verification, and truststore/keystore configuration.
   * <p>
   * When set, {@link #isSsl()}, {@link #getSslSocketFactory()} and {@link #getSslParameters()} are
   * ignored.
   * @return SSL options, or {@code null} to use {@link #isSsl()} configuration
   * @see SslOptions
   * @see SslVerifyMode
   */
  default SslOptions getSslOptions() {
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
