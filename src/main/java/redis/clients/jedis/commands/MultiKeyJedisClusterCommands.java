package redis.clients.jedis.commands;

import redis.clients.jedis.ScanResult;

import java.util.List;

/**
 * @deprecated This interface will be removed in future. Use {@link MultiKeyCommands}.
 */
@Deprecated
public interface MultiKeyJedisClusterCommands extends MultiKeyCommands {

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default List<String> blpop(String... args) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default List<String> brpop(String... args) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default String watch(String... keys) {
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
  default String randomKey() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException use
   * {@link #scan(String, redis.clients.jedis.ScanParams)}.
   */
  @Override
  default ScanResult<String> scan(String cursor) {
    throw new UnsupportedOperationException("Cluster mode only supports SCAN commands with MATCH patterns containing hash-tags");
  }
}
