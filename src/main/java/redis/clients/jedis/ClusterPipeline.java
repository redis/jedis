package redis.clients.jedis;

import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class ClusterPipeline extends MultiNodePipelineBase {

  private final ClusterConnectionProvider provider;
  private AutoCloseable closeable = null;

  public ClusterPipeline(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig),
        createClusterCommandObjects(clientConfig.getRedisProtocol()));
    this.closeable = this.provider;
  }

  public ClusterPipeline(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, poolConfig),
        createClusterCommandObjects(clientConfig.getRedisProtocol()));
    this.closeable = this.provider;
  }

  public ClusterPipeline(ClusterConnectionProvider provider) {
    this(provider, new ClusterCommandObjects());
  }

  public ClusterPipeline(ClusterConnectionProvider provider, ClusterCommandObjects commandObjects) {
    super(commandObjects);
    this.provider = provider;
  }

  private static ClusterCommandObjects createClusterCommandObjects(RedisProtocol protocol) {
    ClusterCommandObjects cco = new ClusterCommandObjects();
    if (protocol == RedisProtocol.RESP3) cco.setProtocol(protocol);
    return cco;
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
    return provider.getNode(((ClusterCommandArguments) args).getCommandHashSlot());
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
