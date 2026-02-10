package redis.clients.jedis.builders;

import java.net.URI;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.JedisAsserts;
import redis.clients.jedis.util.JedisURIHelper;

/**
 * Builder for creating RedisClient instances (standalone Redis connections).
 * <p>
 * This builder provides methods specific to standalone Redis deployments, including host/port
 * configuration, URI-based configuration, and client configuration options.
 * </p>
 */
public abstract class StandaloneClientBuilder<C>
    extends AbstractClientBuilder<StandaloneClientBuilder<C>, C> {

  // Standalone-specific configuration fields
  private HostAndPort hostAndPort = new HostAndPort(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT);

  /**
   * Sets the Redis server host and port.
   * @param host the Redis server hostname
   * @param port the Redis server port
   * @return this builder
   */
  public StandaloneClientBuilder<C> hostAndPort(String host, int port) {
    this.hostAndPort = new HostAndPort(host, port);
    return this;
  }

  /**
   * Sets the Redis server host and port.
   * @param hostAndPort the Redis server host and port
   * @return this builder
   */
  public StandaloneClientBuilder<C> hostAndPort(HostAndPort hostAndPort) {
    this.hostAndPort = hostAndPort;
    return this;
  }

  @Override
  protected StandaloneClientBuilder<C> self() {
    return this;
  }

  @Override
  protected ConnectionProvider createDefaultConnectionProvider() {
    return new PooledConnectionProvider(this.hostAndPort, this.clientConfig, this.cache,
        this.poolConfig);
  }

  @Override
  protected void validateSpecificConfiguration() {
    validateCommonConfiguration();

    if (hostAndPort == null) {
      throw new IllegalArgumentException("Either URI or host/port must be specified");
    }
  }

  /**
   * Sets the Redis server URI from a string.
   * <p>
   * This method extracts connection parameters from the URI and merges them into the current client
   * configuration. If a client configuration was previously set via
   * {@link #clientConfig(JedisClientConfig)}, only the values explicitly provided in the URI will
   * override the existing configuration. Values not present in the URI will be preserved from the
   * existing configuration.
   * <p>
   * <b>This method sets:</b>
   * <ul>
   * <li>Host and port from the URI (always set)</li>
   * <li>Client configuration with URI-derived values (merged with existing config if present)</li>
   * </ul>
   * <p>
   * <b>URI values override existing config values when:</b>
   * <ul>
   * <li>URI contains user/password - overrides existing credentials</li>
   * <li>URI contains database - overrides existing database</li>
   * <li>URI contains protocol parameter - overrides existing protocol</li>
   * <li>URI uses rediss:// scheme - enables SSL</li>
   * </ul>
   * <p>
   * <b>Examples:</b>
   * 
   * <pre>
   * // Credentials from config are preserved (URI has no credentials)
   * builder.clientConfig(configWithCredentials).fromURI("redis://localhost:6379")
   *
   * // URI credentials override config credentials
   * builder.clientConfig(configWithCredentials).fromURI("redis://user:pass@localhost:6379")
   *
   * // Config completely overrides URI (last wins)
   * builder.fromURI("redis://user:pass@localhost:6379").clientConfig(newConfig)
   * </pre>
   *
   * @param uriString the Redis server URI string
   * @return this builder
   * @deprecated Use {@link #hostAndPort(String, int)} combined with
   *             {@link #clientConfig(JedisClientConfig)} for explicit configuration. This method
   *             will be removed in Jedis 8.0.0.
   *             <p>
   *             <b>Migration example:</b>
   * 
   *             <pre>
   *             // Old (deprecated):
   *             builder.fromURI("redis://user:pass@localhost:6379/2")
   *
   *             // New (recommended):
   *             builder.hostAndPort("localhost", 6379)
   *                    .clientConfig(DefaultJedisClientConfig.builder()
   *                        .user("user")
   *                        .password("pass")
   *                        .database(2)
   *                        .build())
   *             </pre>
   */
  @Deprecated
  public StandaloneClientBuilder<C> fromURI(String uriString) {
    return fromURI(URI.create(uriString));
  }

  /**
   * Sets the Redis server URI.
   * <p>
   * This method extracts connection parameters from the URI and merges them into the current client
   * configuration. If a client configuration was previously set via
   * {@link #clientConfig(JedisClientConfig)}, only the values explicitly provided in the URI will
   * override the existing configuration. Values not present in the URI will be preserved from the
   * existing configuration.
   * <p>
   * <b>This method sets:</b>
   * <ul>
   * <li>Host and port from the URI (always set)</li>
   * <li>Client configuration with URI-derived values (merged with existing config if present)</li>
   * </ul>
   * <p>
   * <b>URI values override existing config values when:</b>
   * <ul>
   * <li>URI contains user/password - overrides existing credentials</li>
   * <li>URI contains database - overrides existing database</li>
   * <li>URI contains protocol parameter - overrides existing protocol</li>
   * <li>URI uses rediss:// scheme - enables SSL</li>
   * </ul>
   * <p>
   * <b>Examples:</b>
   * 
   * <pre>
   * // Credentials from config are preserved (URI has no credentials)
   * builder.clientConfig(configWithCredentials).fromURI(uri)
   *
   * // URI credentials override config credentials
   * builder.clientConfig(configWithCredentials).fromURI(uriWithCredentials)
   *
   * // Config completely overrides URI (last wins)
   * builder.fromURI(uriWithCredentials).clientConfig(newConfig)
   * </pre>
   *
   * @param uri the Redis server URI
   * @return this builder
   * @deprecated Use {@link #hostAndPort(HostAndPort)} combined with
   *             {@link #clientConfig(JedisClientConfig)} for explicit configuration. This method
   *             will be removed in Jedis 8.0.0. See {@link #fromURI(String)} for migration example.
   */
  @Deprecated
  public StandaloneClientBuilder<C> fromURI(URI uri) {
    JedisAsserts.notNull(uri, "Redis URI must not be null");
    JedisAsserts.isTrue(JedisURIHelper.isValid(uri), "Invalid Redis URI");

    // Start with existing config if present, otherwise create new builder
    DefaultJedisClientConfig.Builder configBuilder = (this.clientConfig != null)
        ? DefaultJedisClientConfig.builder().from(this.clientConfig)
        : DefaultJedisClientConfig.builder();

    // Override with URI values only if URI provides them (non-null)
    String uriUser = JedisURIHelper.getUser(uri);
    String uriPassword = JedisURIHelper.getPassword(uri);

    // If URI provides credentials, we need to clear the credentialsProvider
    // so that the new user/password values are used instead
    if (uriUser != null || uriPassword != null) {
      configBuilder.credentialsProvider(null);
      if (uriUser != null) {
        configBuilder.user(uriUser);
      }
      if (uriPassword != null) {
        configBuilder.password(uriPassword);
      }
    }

    if (JedisURIHelper.hasDbIndex(uri)) {
      configBuilder.database(JedisURIHelper.getDBIndex(uri));
    }

    RedisProtocol uriProtocol = JedisURIHelper.getRedisProtocol(uri);
    if (uriProtocol != null) {
      configBuilder.protocol(uriProtocol);
    }

    if (JedisURIHelper.isRedisSSLScheme(uri)) {
      configBuilder.ssl(true);
    } else if (JedisURIHelper.isRedisScheme(uri)) {
      configBuilder.ssl(false);
    }

    this.clientConfig = configBuilder.build();
    return hostAndPort(JedisURIHelper.getHostAndPort(uri));
  }
}
