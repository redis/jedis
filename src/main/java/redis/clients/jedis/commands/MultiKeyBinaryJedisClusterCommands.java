package redis.clients.jedis.commands;

import redis.clients.jedis.ScanResult;

import java.util.List;

/**
 * @deprecated This interface will be removed in future. Use {@link MultiKeyBinaryCommands}.
 */
@Deprecated
public interface MultiKeyBinaryJedisClusterCommands extends MultiKeyBinaryCommands {

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default List<byte[]> blpop(byte[]... args) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default List<byte[]> brpop(byte[]... args) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default String watch(byte[]... keys) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default String unwatch() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default byte[] randomBinaryKey() {
    throw new UnsupportedOperationException();
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
