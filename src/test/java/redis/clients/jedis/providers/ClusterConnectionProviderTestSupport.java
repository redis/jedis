package redis.clients.jedis.providers;

import redis.clients.jedis.JedisClusterInfoCache;

/** Test bridge to the package-private internals of {@link ClusterConnectionProvider}. */
public final class ClusterConnectionProviderTestSupport {

  private ClusterConnectionProviderTestSupport() {
  }

  /** The provider's slot/node cache. */
  public static JedisClusterInfoCache cache(ClusterConnectionProvider provider) {
    return provider.cache;
  }
}
