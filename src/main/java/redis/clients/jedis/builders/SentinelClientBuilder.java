package redis.clients.jedis.builders;

import java.util.Set;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;

/**
 * Builder for creating JedisSentineled instances (Redis Sentinel connections).
 * <p>
 * This builder provides methods specific to Redis Sentinel deployments, including master name
 * configuration, sentinel nodes configuration, and separate client configurations for master and
 * sentinel connections.
 * </p>
 */
public abstract class SentinelClientBuilder<C>
    extends AbstractClientBuilder<SentinelClientBuilder<C>, C> {

  // Sentinel-specific configuration fields
  private String masterName = null;
  private Set<HostAndPort> sentinels = null;
  private JedisClientConfig sentinelClientConfig = null;

  /**
   * Sets the master name for the Redis Sentinel configuration.
   * <p>
   * This is the name of the Redis master as configured in the Sentinel instances. The Sentinel will
   * monitor this master and provide failover capabilities.
   * @param masterName the master name (must not be null or empty)
   * @return this builder
   */
  public SentinelClientBuilder<C> masterName(String masterName) {
    this.masterName = masterName;
    return this;
  }

  /**
   * Sets the sentinel nodes to connect to.
   * <p>
   * At least one sentinel must be specified. The client will use these sentinels to discover the
   * current master and monitor for failover events.
   * @param sentinels the set of sentinel nodes
   * @return this builder
   */
  public SentinelClientBuilder<C> sentinels(Set<HostAndPort> sentinels) {
    this.sentinels = sentinels;
    return this;
  }

  /**
   * Sets the client configuration for Sentinel connections.
   * <p>
   * This configuration is used for connections to the Sentinel instances. It may have different
   * authentication credentials and settings than the master connections.
   * @param sentinelClientConfig the client configuration for sentinel connections
   * @return this builder
   */
  public SentinelClientBuilder<C> sentinelClientConfig(JedisClientConfig sentinelClientConfig) {
    this.sentinelClientConfig = sentinelClientConfig;
    return this;
  }

  @Override
  protected SentinelClientBuilder<C> self() {
    return this;
  }

  @Override
  protected ConnectionProvider createDefaultConnectionProvider() {
    return new SentineledConnectionProvider(this.masterName, this.clientConfig, this.cache,
        this.poolConfig, this.sentinels, this.sentinelClientConfig);
  }

  @Override
  protected void validateSpecificConfiguration() {
    validateCommonConfiguration();

    if (masterName == null || masterName.trim().isEmpty()) {
      throw new IllegalArgumentException("Master name is required for Sentinel mode");
    }

    if (sentinels == null || sentinels.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one sentinel must be specified for Sentinel mode");
    }
  }

  @Override
  public C build() {
    if (sentinelClientConfig == null) {
      sentinelClientConfig = DefaultJedisClientConfig.builder().build();
    }

    return super.build();
  }

}
