package redis.clients.jedis;

import java.util.Map;
import java.util.function.Function;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.util.JedisClusterCRC16;

public abstract class Retryer {
  protected final JedisClusterConnectionHandler connectionHandler;

  public Retryer(JedisClusterConnectionHandler connectionHandler) {
    this.connectionHandler = connectionHandler;
  }

  /**
   * Execute a Redis command with retries.
   *
   * @param slot From one of the {@code JedisClusterCRC16#get*())} methods
   * @return The result of running the given command with retries
   */
  protected abstract <R> R runWithRetries(int slot, Function<Jedis, R> command);

  /**
   * Execute a Redis command on any random node.
   */
  public abstract <R> R runWithRetries(Function<Jedis, R> command);

  /**
   * Override this method to free up any resources.
   */
  public void close() {
    // This method intentionally left blank
  }

  public Map<String, JedisPool> getClusterNodes() {
    return connectionHandler.getNodes();
  }

  public Jedis getConnectionFromSlot(int slot) {
    return connectionHandler.getConnectionFromSlot(slot);
  }

  public <R> R run(Function<Jedis, R> command, String key) {
    return runWithRetries(JedisClusterCRC16.getSlot(key), command);
  }

  public <R> R run(Function<Jedis, R> command, int keyCount, String... keys) {
    if (keys == null || keys.length == 0) {
      throw new JedisClusterOperationException("No way to dispatch this command to Redis Cluster.");
    }

    // For multiple keys, only execute if they all share the same connection slot.
    int slot = JedisClusterCRC16.getSlot(keys[0]);
    if (keys.length > 1) {
      for (int i = 1; i < keyCount; i++) {
        int nextSlot = JedisClusterCRC16.getSlot(keys[i]);
        if (slot != nextSlot) {
          throw new JedisClusterOperationException("No way to dispatch this command to Redis "
              + "Cluster because keys have different slots.");
        }
      }
    }

    return runWithRetries(slot, command);
  }

  public <R> R runBinary(Function<Jedis, R> command, byte[] key) {
    return runWithRetries(JedisClusterCRC16.getSlot(key), command);
  }

  public <R> R runBinary(Function<Jedis, R> command, int keyCount, byte[]... keys) {
    if (keys == null || keys.length == 0) {
      throw new JedisClusterOperationException("No way to dispatch this command to Redis Cluster.");
    }

    // For multiple keys, only execute if they all share the same connection slot.
    int slot = JedisClusterCRC16.getSlot(keys[0]);
    if (keys.length > 1) {
      for (int i = 1; i < keyCount; i++) {
        int nextSlot = JedisClusterCRC16.getSlot(keys[i]);
        if (slot != nextSlot) {
          throw new JedisClusterOperationException("No way to dispatch this command to Redis "
              + "Cluster because keys have different slots.");
        }
      }
    }

    return runWithRetries(slot, command);
  }
}
