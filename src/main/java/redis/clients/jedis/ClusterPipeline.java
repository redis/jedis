package redis.clients.jedis;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.IOUtils;

/**
 * Pipeline implementation for Redis Cluster mode.
 * <p>
 * ClusterPipeline allows batching multiple commands for efficient execution in a Redis Cluster
 * environment. Commands are automatically routed to the appropriate cluster nodes based on
 * key hash slots.
 * </p>
 * <p>
 * <strong>Important Limitations:</strong>
 * </p>
 * <ul>
 * <li><strong>Single-node commands only:</strong> Only commands that can be routed to a single
 * node are supported. Commands requiring execution on multiple nodes (ALL_SHARDS, MULTI_SHARD,
 * ALL_NODES, or SPECIAL request policies) will throw {@link UnsupportedOperationException}.</li>
 * <li><strong>Examples of unsupported commands:</strong>
 *   <ul>
 *     <li>{@code KEYS} - requires execution on all master shards</li>
 *     <li>{@code MGET} with keys in different slots - requires execution on multiple shards</li>
 *     <li>{@code SCRIPT LOAD} - requires execution on all nodes</li>
 *   </ul>
 * </li>
 * <li>For multi-node commands, use the non-pipelined mode
 * of {@link RedisClusterClient} instead.</li>
 * </ul>
 * <p>
 * <strong> Usage Pattern:</strong>
 * </p>
 * <pre>{@code
 * try (RedisCluster cluster = new RedisCluster(nodes, config)) {
 *   // For single-node commands, use pipelined mode
 *   try (ClusterPipeline pipeline = cluster.pipelined()) {
 *     Response<String> r1 = pipeline.set("key1", "value1");
 *     Response<String> r2 = pipeline.get("key1");
 *     pipeline.sync();
 *
 *     System.out.println(r1.get()); // "OK"
 *     System.out.println(r2.get()); // "value1"
 *   }
 *
 *   // For multi-node commands, use non-pipelined mode
 *   Set<String> allKeys = cluster.keys("*"); // Executes on all master shards
 *   List<String> values = cluster.mget("key1", "key2", "key3"); // Cross-slot keys
 * }
 * }</pre>
 *
 * @see MultiNodePipelineBase
 * @see redis.clients.jedis.RedisClusterClient
 */
public class ClusterPipeline extends MultiNodePipelineBase {

  private final ClusterConnectionProvider provider;
  private AutoCloseable closeable = null;

  public ClusterPipeline(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig), clientConfig);
    this.closeable = this.provider;
  }

  public ClusterPipeline(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, poolConfig), clientConfig);
    this.closeable = this.provider;
  }

  public ClusterPipeline(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Duration topologyRefreshPeriod) {
    this(new ClusterConnectionProvider(clusterNodes, clientConfig, poolConfig, topologyRefreshPeriod),
        clientConfig);
    this.closeable = this.provider;
  }

  private ClusterPipeline(ClusterConnectionProvider provider, JedisClientConfig clientConfig) {
    this(provider, createClusterCommandObjects(provider, clientConfig.getRedisProtocol(),
        clientConfig.isAutoNegotiateProtocol()));
  }

  public ClusterPipeline(ClusterConnectionProvider provider, RedisProtocol protocol) {
    this(provider, createClusterCommandObjects(provider, protocol, true));
  }

  public ClusterPipeline(ClusterConnectionProvider provider, ClusterCommandObjects commandObjects) {
    this(provider, () -> commandObjects);
  }

  private ClusterPipeline(ClusterConnectionProvider provider,
      Supplier<ClusterCommandObjects> commandObjectsSupplier) {
    super(commandObjectsSupplier.get());
    this.provider = provider;
  }

  ClusterPipeline(ClusterConnectionProvider provider, ClusterCommandObjects commandObjects,
          CommandFlagsRegistry commandFlagsRegistry, ExecutorService executorService) {
    super(commandObjects, commandFlagsRegistry, executorService);
    this.provider = provider;
  }

  private static Supplier<ClusterCommandObjects> createClusterCommandObjects(
      ClusterConnectionProvider provider, RedisProtocol protocol, boolean autoNegotiateProtocol) {
    return () -> {
      RedisProtocol effective = (protocol == RedisProtocol.RESP3) ? protocol : null;

      boolean protocolResolvedOnWire = protocol == null && autoNegotiateProtocol;
      if (protocolResolvedOnWire) {
        try (Connection conn = provider.getConnection()) {
          RedisProtocol resolved = conn.getRedisProtocol();
          if (resolved != null) {
            effective = resolved;
          }
        } catch (JedisException je) {
          // Protocol negotiation failed, leave protocol=null on the command objects.
        }
      }
      return new ClusterCommandObjects(effective);
    };
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
