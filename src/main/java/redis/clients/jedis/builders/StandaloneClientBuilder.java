package redis.clients.jedis.builders;

import java.net.URI;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.JedisURIHelper;

/**
 * Builder for creating JedisPooled instances (standalone Redis connections).
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
    JedisClientConfig config = this.clientConfig != null ? this.clientConfig
        : DefaultJedisClientConfig.builder().build();
    return new PooledConnectionProvider(this.hostAndPort, config, this.cache, this.poolConfig);
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
   * @param uriString the Redis server URI string
   * @return this builder
   */
  public StandaloneClientBuilder<C> fromURI(String uriString) {
    return fromURI(URI.create(uriString));
  }

  /**
   * Sets the Redis server URI.
   * @param uri the Redis server URI
   * @return this builder
   */
  public StandaloneClientBuilder<C> fromURI(URI uri) {
    this.clientConfig = DefaultJedisClientConfig.builder().user(JedisURIHelper.getUser(uri))
        .password(JedisURIHelper.getPassword(uri)).database(JedisURIHelper.getDBIndex(uri))
        .protocol(JedisURIHelper.getRedisProtocol(uri)).ssl(JedisURIHelper.isRedisSSLScheme(uri))
        .build();
    return hostAndPort(JedisURIHelper.getHostAndPort(uri));
  }
}
