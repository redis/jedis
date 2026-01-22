package redis.clients.jedis;

import java.time.Duration;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
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

  public ClusterPipeline(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Duration topologyRefreshPeriod) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, poolConfig, topologyRefreshPeriod),
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
    Set<Integer> slots = args.getKeyHashSlots();

    if (slots.size() > 1) {
      throw new JedisClusterOperationException("Cannot get NodeKey for command with multiple hash slots");
    }

    if (slots.isEmpty()) {
      return null; // Let getConnection(null) handle it by using a random node
    }

    return provider.getNode(slots.iterator().next());
  }

  @Override
  protected Connection getConnection(HostAndPort nodeKey) {
    return provider.getConnection(nodeKey);
  }

  public Response<Long> spublish(String channel, String message) {
    return appendCommand(commandObjects.spublish(channel, message));
  }

  public Response<Long> spublish(byte[] channel, byte[] message) {
    return appendCommand(commandObjects.spublish(channel, message));
  }
}
