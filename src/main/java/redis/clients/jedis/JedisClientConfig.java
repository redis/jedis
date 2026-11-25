package redis.clients.jedis;

import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.authentication.AuthXManager;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.search.SearchProtocol;

public interface JedisClientConfig {

  /**
   * The protocol configured for the connection, or {@code null} (default) if none is configured.
   * <p>
   * Together with {@link #isAutoNegotiateProtocol()} it decides which protocol the connection uses.
   * The supported configurations:
   * </p>
   * <table border="1">
   * <caption>Protocol and auto-negotiation combinations</caption>
   * <tr>
   * <th>protocol</th>
   * <th>autoNegotiateProtocol</th>
   * <th>Semantics</th>
   * <th>Description</th>
   * </tr>
   * <tr>
   * <td>{@code RESP2}</td>
   * <td>ignored</td>
   * <td>Strict protocol</td>
   * <td>Uses RESP2. The {@code HELLO} command is required to enforce it.</td>
   * </tr>
   * <tr>
   * <td>{@code RESP3}</td>
   * <td>ignored</td>
   * <td>Strict protocol</td>
   * <td>Uses RESP3. The {@code HELLO} command is required to enforce it.</td>
   * </tr>
   * <tr>
   * <td>{@code null}</td>
   * <td>{@code true}</td>
   * <td>Negotiated protocol</td>
   * <td>Tries RESP3, then RESP2. If the protocol cannot be enforced, falls back to the server
   * default (assumes RESP2).</td>
   * </tr>
   * <tr>
   * <td>{@code null}</td>
   * <td>{@code false}</td>
   * <td>Server default protocol</td>
   * <td>The protocol is not enforced and the client assumes RESP2.</td>
   * </tr>
   * </table>
   * @return the configured protocol, or {@code null}
   */
  default RedisProtocol getRedisProtocol() {
    return null;
  }

  /**
   * Whether the connection negotiates the protocol with the server. This setting is ignored when a
   * strict protocol is configured with {@link #getRedisProtocol()}.
   * <p>
   * When {@code true} (default), the connection negotiates RESP3, then RESP2. If the protocol
   * cannot be negotiated (for example, the server does not support the {@code HELLO} command), it
   * falls back to the server default and assumes RESP2.
   * </p>
   * <p>
   * When {@code false} and no strict protocol is configured, the connection uses the server
   * default: the protocol is not enforced and the client assumes RESP2.
   * </p>
   * <p>
   * See {@link #getRedisProtocol()} for the supported configurations.
   * </p>
   * <p>
   * The legacy {@link Jedis} client does not support auto-negotiation: when this setting is
   * {@code true}, it uses the server default instead and logs a warning.
   * </p>
   * @return {@code true} to negotiate the protocol (default), {@code false} to use the server
   *         default
   */
  default boolean isAutoNegotiateProtocol() {
    return true;
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

  /**
   * Optional key argument pre-processor applied to every command before it is sent. Returning
   * {@code null} disables key rewriting.
   * <p>
   * <b>Not supported by the legacy {@link Jedis} class</b> — only the {@link UnifiedJedis} family
   * ({@link RedisClient}, {@link RedisClusterClient}, {@link RedisSentinelClient},
   * {@link MultiDbClient}) honours this value.
   */
  default CommandKeyArgumentPreProcessor getCommandKeyArgumentPreProcessor() {
    return null;
  }

  /**
   * Optional JSON object mapper used by RedisJSON commands. Returning {@code null} falls back to
   * the library default (Gson-based).
   * <p>
   * <b>Not supported by the legacy {@link Jedis} class</b> — only the {@link UnifiedJedis} family
   * ({@link RedisClient}, {@link RedisClusterClient}, {@link RedisSentinelClient},
   * {@link MultiDbClient}) honours this value.
   */
  default JsonObjectMapper getJsonObjectMapper() {
    return null;
  }

  /**
   * Default search dialect applied to RediSearch commands when the caller does not specify one.
   * Defaults to {@link SearchProtocol#DEFAULT_DIALECT}.
   * <p>
   * <b>Not supported by the legacy {@link Jedis} class</b> — only the {@link UnifiedJedis} family
   * ({@link RedisClient}, {@link RedisClusterClient}, {@link RedisSentinelClient},
   * {@link MultiDbClient}) honours this value.
   */
  default int getSearchDialect() {
    return SearchProtocol.DEFAULT_DIALECT;
  }

}
