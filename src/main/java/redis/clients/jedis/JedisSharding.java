package redis.clients.jedis;

import redis.clients.jedis.providers.ShardedConnectionProvider;
import redis.clients.jedis.util.Hashing;
import today.bonfire.oss.sop.SimpleObjectPoolConfig;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @deprecated Sharding/Sharded feature will be removed in next major release.
 */
@Deprecated
public class JedisSharding extends UnifiedJedis {

  public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern.compile("\\{(.+?)\\}");

  public JedisSharding(List<HostAndPort> shards) {
    this(new ShardedConnectionProvider(shards));
  }

  public JedisSharding(List<HostAndPort> shards, JedisClientConfig clientConfig) {
    this(new ShardedConnectionProvider(shards, clientConfig));
    setProtocol(clientConfig);
  }

  public JedisSharding(List<HostAndPort> shards, JedisClientConfig clientConfig,
                       SimpleObjectPoolConfig poolConfig) {
    this(new ShardedConnectionProvider(shards, clientConfig, poolConfig));
    setProtocol(clientConfig);
  }

  public JedisSharding(List<HostAndPort> shards, JedisClientConfig clientConfig, Hashing algo) {
    this(new ShardedConnectionProvider(shards, clientConfig, algo));
    setProtocol(clientConfig);
  }

  public JedisSharding(List<HostAndPort> shards, JedisClientConfig clientConfig,
                       SimpleObjectPoolConfig poolConfig, Hashing algo) {
    this(new ShardedConnectionProvider(shards, clientConfig, poolConfig, algo));
    setProtocol(clientConfig);
  }

  public JedisSharding(ShardedConnectionProvider provider) {
    super(provider);
  }

  public JedisSharding(ShardedConnectionProvider provider, Pattern tagPattern) {
    super(provider, tagPattern);
  }

  private void setProtocol(JedisClientConfig clientConfig) {
    RedisProtocol proto = clientConfig.getRedisProtocol();
    if (proto == RedisProtocol.RESP3) commandObjects.setProtocol(proto);
  }

  @Override
  public ShardedPipeline pipelined() {
    return new ShardedPipeline((ShardedConnectionProvider) provider);
  }

  /**
   * @param doMulti param
   * @return nothing
   */
  @Override
  public AbstractTransaction transaction(boolean doMulti) {
    throw new UnsupportedOperationException();
  }
}
