package redis.clients.jedis.commands;

import redis.clients.jedis.ScanResult;

import java.util.List;

/**
 * @deprecated This interface will be removed in future. Use {@link MultiKeyCommands}.
 */
@Deprecated
public interface MultiKeyJedisClusterCommands extends MultiKeyCommands {

  /**
   * @throws UnsupportedOperationException Use {@link #copy(java.lang.String, java.lang.String, boolean)}.
   */
  @Override
  default boolean copy(String srcKey, String dstKey, int db, boolean replace) {
    throw new UnsupportedOperationException("Cluster mode does not support databse operations.");
  }

  /**
   * @throws UnsupportedOperationException Use {@link #blpop(double, java.lang.String...)} or
   * {@link #blpop(int, java.lang.String...)}.
   */
  @Override
  default List<String> blpop(String... args) {
    throw new UnsupportedOperationException("Use other versions of BLPOP.");
  }

  /**
   * @throws UnsupportedOperationException Use {@link #brpop(double, java.lang.String...)} or
   * {@link #brpop(int, java.lang.String...)}.
   */
  @Override
  default List<String> brpop(String... args) {
    throw new UnsupportedOperationException("Use other versions of BRPOP");
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default String watch(String... keys) {
    throw new UnsupportedOperationException("WATCH in cluster mode is not supproted yet.");
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  default String randomKey() {
    throw new UnsupportedOperationException("RANDOMKEY in cluster mode is not supproted yet.");
  }

  /**
   * @throws UnsupportedOperationException Use {@link #scan(String, redis.clients.jedis.ScanParams)}.
   */
  @Override
  default ScanResult<String> scan(String cursor) {
    throw new UnsupportedOperationException("Cluster mode only supports SCAN commands with MATCH patterns containing hash-tags.");
  }
}
