package redis.clients.jedis.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.ShardedCommandArguments;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Hashing;

public class ShardedConnectionProvider implements ConnectionProvider {

  private final TreeMap<Long, HostAndPort> nodes = new TreeMap<>();
  private final Map<String, ConnectionPool> resources = new HashMap<>();
  private final JedisClientConfig clientConfig;
  private final GenericObjectPoolConfig<Connection> poolConfig;
  private final Hashing algo;

  public ShardedConnectionProvider(List<HostAndPort> shards) {
    this(shards, DefaultJedisClientConfig.builder().build());
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig) {
    this(shards, clientConfig, new GenericObjectPoolConfig<Connection>());
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(shards, clientConfig, poolConfig, Hashing.MURMUR_HASH);
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig,
      Hashing algo) {
    this(shards, clientConfig, new GenericObjectPoolConfig<Connection>(), algo);
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Hashing algo) {
    this.clientConfig = clientConfig;
    this.poolConfig = poolConfig;
    this.algo = algo;
    initialize(shards);
  }

  private void initialize(List<HostAndPort> shards) {
    for (int i = 0; i < shards.size(); i++) {
      HostAndPort shard = shards.get(i);
      for (int n = 0; n < 160; n++) {
        Long hash = this.algo.hash("SHARD-" + i + "-NODE-" + n);
        nodes.put(hash, shard);
        setupNodeIfNotExist(shard);
      }
    }
  }

  private ConnectionPool setupNodeIfNotExist(final HostAndPort node) {
    String nodeKey = node.toString();
    ConnectionPool existingPool = resources.get(nodeKey);
    if (existingPool != null) return existingPool;

    ConnectionPool nodePool = new ConnectionPool(node, clientConfig, poolConfig);
    resources.put(nodeKey, nodePool);
    return nodePool;
  }

  public Hashing getHashingAlgo() {
    return algo;
  }

  private void reset() {
    for (ConnectionPool pool : resources.values()) {
      try {
        if (pool != null) {
          pool.destroy();
        }
      } catch (RuntimeException e) {
        // pass
      }
    }
    resources.clear();
    nodes.clear();
  }

  @Override
  public void close() {
    reset();
  }

  public HostAndPort getNode(Long hash) {
    return hash != null ? getNodeFromHash(hash) : null;
  }

  public Connection getConnection(HostAndPort node) {
    return node != null ? setupNodeIfNotExist(node).getResource() : getConnection();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    final Long hash = ((ShardedCommandArguments) args).getKeyHash();
    return hash != null ? getConnection(getNodeFromHash(hash)) : getConnection();
  }

  private List<ConnectionPool> getShuffledNodesPool() {
    List<ConnectionPool> pools = new ArrayList<>(resources.values());
    Collections.shuffle(pools);
    return pools;
  }

  @Override
  public Connection getConnection() {
    List<ConnectionPool> pools = getShuffledNodesPool();

    JedisException suppressed = null;
    for (ConnectionPool pool : pools) {
      Connection jedis = null;
      try {
        jedis = pool.getResource();
        if (jedis == null) {
          continue;
        }

        jedis.ping();
        return jedis;

      } catch (JedisException ex) {
        if (suppressed == null) { // remembering first suppressed exception
          suppressed = ex;
        }
        if (jedis != null) {
          jedis.close();
        }
      }
    }

    JedisException noReachableNode = new JedisException("No reachable shard.");
    if (suppressed != null) {
      noReachableNode.addSuppressed(suppressed);
    }
    throw noReachableNode;
  }

  private HostAndPort getNodeFromHash(Long hash) {
    SortedMap<Long, HostAndPort> tail = nodes.tailMap(hash);
    if (tail.isEmpty()) {
      return nodes.get(nodes.firstKey());
    }
    return tail.get(tail.firstKey());
  }
}
