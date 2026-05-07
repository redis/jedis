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
   * Whether TLS/SSL should be used for connections.
   * <p>
   * A TLS connection is established when this returns {@code true} or when {@link #getSslOptions()}
   * returns a non-{@code null} value. If both are provided, {@link #getSslOptions()} takes
   * precedence.
   * @return {@code true} if TLS/SSL is enabled, {@code false} otherwise
   * @see #getSslOptions()
   */
  default boolean isSsl() {
    return false;
  }

  /**
   * Custom {@link SSLSocketFactory} to use for TLS connections.
   * <p>
   * Consulted only when {@link #getSslOptions()} returns {@code null}. Implementations should
   * return {@code null} to use the JVM default.
   * @return custom SSL socket factory, or {@code null} to use the default
   * @see #getSslOptions()
   * @deprecated since 7.4.2, use {@link #getSslOptions()} instead.
   */
  @Deprecated
  default SSLSocketFactory getSslSocketFactory() {
    return null;
  }

  /**
   * Custom {@link SSLParameters} to apply to TLS sockets.
   * <p>
   * Consulted only when {@link #getSslOptions()} returns {@code null}. Implementations should
   * return {@code null} to let the client apply defaults (which enable HTTPS hostname
   * verification).
   * @return custom SSL parameters, or {@code null} for defaults
   * @see #getSslOptions()
   * @deprecated since 7.4.2, use {@link #getSslOptions()} instead.
   */
  @Deprecated
  default SSLParameters getSslParameters() {
    return null;
  }

  /**
   * TLS/SSL configuration. Recommended way to configure TLS connections.
   * <p>
   * When non-{@code null}, TLS is enabled and this takes precedence over
   * {@link #getSslSocketFactory()} and {@link #getSslParameters()}. Implementations should return
   * {@code null} to fall back to {@link #isSsl()} / {@link #getSslSocketFactory()} /
   * {@link #getSslParameters()}.
   * @return TLS configuration, or {@code null} if not configured
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
