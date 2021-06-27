package redis.clients.jedis.commands;

import redis.clients.jedis.ScanResult;

import java.util.List;

/**
 * @deprecated This interface will be removed in future. Use {@link MultiKeyBinaryCommands}.
 */
@Deprecated
public interface MultiKeyBinaryJedisClusterCommands extends MultiKeyBinaryCommands {

  /**
   * @throws UnsupportedOperationException Use {@link #copy(byte[], byte[], boolean)}.
   */
  @Override
  default boolean copy(byte[] srcKey, byte[] dstKey, int db, boolean replace) {
    throw new UnsupportedOperationException("Cluster mode does not support databse operations.");
  }

  /**
   * @throws UnsupportedOperationException Use {@link #blpop(double, byte[]...)} or
   * {@link #blpop(int, byte[]...)}.
   */
  @Override
  default List<byte[]> blpop(byte[]... args) {
    throw new UnsupportedOperationException("Use other versions of BLPOP.");
  }

  /**
   * @throws UnsupportedOperationException Use {@link #brpop(double, byte[]...)} or{
   * {@link #brpop(int, byte[]...)}.
   */
  @Override
  default List<byte[]> brpop(byte[]... args) {
    throw new UnsupportedOperationException("Use other versions of BRPOP");
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default String watch(byte[]... keys) {
    throw new UnsupportedOperationException("WATCH in cluster mode is not supported yet.");
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default byte[] randomBinaryKey() {
    throw new UnsupportedOperationException("RANDOMKEY in cluster mode is not supproted yet.");
  }

  /**
   * @throws UnsupportedOperationException use
   * {@link #scan(byte[], redis.clients.jedis.ScanParams)}.
   */
  @Override
  default ScanResult<byte[]> scan(byte[] cursor) {
    throw new UnsupportedOperationException("Cluster mode only supports SCAN commands with MATCH patterns containing hash-tags");
  }
}
