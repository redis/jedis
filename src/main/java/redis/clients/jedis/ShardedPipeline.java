package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.providers.ShardedConnectionProvider;
import redis.clients.jedis.util.Hashing;
import redis.clients.jedis.util.IOUtils;

public class ShardedPipeline extends MultiNodePipelineBase {

  private final ShardedConnectionProvider provider;
  private AutoCloseable closeable = null;

  public ShardedPipeline(List<HostAndPort> shards, JedisClientConfig clientConfig) {
    this(new ShardedConnectionProvider(shards, clientConfig));
    this.closeable = this.provider;
  }

  public ShardedPipeline(ShardedConnectionProvider provider) {
    super(new ShardedCommandObjects(provider.getHashingAlgo()));
    this.provider = provider;
  }

  public ShardedPipeline(List<HostAndPort> shards, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Hashing algo, Pattern tagPattern) {
    this(new ShardedConnectionProvider(shards, clientConfig, poolConfig, algo), tagPattern);
    this.closeable = this.provider;
  }

  public ShardedPipeline(ShardedConnectionProvider provider, Pattern tagPattern) {
    super(new ShardedCommandObjects(provider.getHashingAlgo(), tagPattern));
    this.provider = provider;
  }

  @Override
  public void close() {
    try {
      super.close();
    } finally {
      IOUtils.closeQuietly(closeable);
    }
  }

  @Override
  protected HostAndPort getNodeKey(CommandArguments args) {
    return provider.getNode(((ShardedCommandArguments) args).getKeyHash());
  }

  @Override
  protected Connection getConnection(HostAndPort nodeKey) {
    return provider.getConnection(nodeKey);
  }

  /**
   * This method must be called after constructor, if graph commands are going to be used.
   */
  public void prepareGraphCommands() {
    super.prepareGraphCommands(provider);
  }
}
