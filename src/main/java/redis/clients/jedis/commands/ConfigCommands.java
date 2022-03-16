package redis.clients.jedis.commands;

import java.util.List;

/**
 * The interface about managing configuration parameters of Redis server.
 */
public interface ConfigCommands {

  /**
   * Used to read the configuration parameters of Redis server.
   *
   * @param pattern name of Redis server's configuration
   * @return config value of Redis server
   */
  List<String> configGet(String pattern);

  /**
   * Used to read the configuration parameters of Redis server.
   *
   * @param patterns names of Redis server's configuration
   * @return values of Redis server's configuration
   */
  List<String> configGet(String... patterns);

  /**
   * Used to read the configuration parameters of Redis server.
   *
   * @param pattern name of Redis server's configuration
   * @return value of Redis server's configuration
   */
  List<byte[]> configGet(byte[] pattern);

  /**
   * Used to read the configuration parameters of Redis server.
   *
   * @param patterns names of Redis server's configuration
   * @return values of Redis server's configuration
   */
  List<byte[]> configGet(byte[]... patterns);

  /**
   * Used in order to reconfigure the Redis server at run time without
   * the need to restart.
   *
   * @param parameter name of Redis server's configuration
   * @param value     value of Redis server's configuration
   * @return OK when the configuration was set properly.
   * Otherwise, an error is returned.
   */
  String configSet(String parameter, String value);

  String configSet(String... parameterValues);

  /**
   * Used in order to reconfigure the Redis server at run time without
   * the need to restart.
   *
   * @param parameter name of Redis server's configuration
   * @param value     value of Redis server's configuration
   * @return OK when the configuration was set properly.
   * Otherwise, an error is returned.
   */
  String configSet(byte[] parameter, byte[] value);

  String configSet(byte[]... parameterValues);

  /**
   * Resets the statistics reported by Redis using the INFO command.
   * <p>
   * These are the counters that are reset:
   * <p>
   * 1) Keyspace hits
   * 2) Keyspace misses
   * 3) Number of commands processed
   * 4) Number of connections received
   * 5) Number of expired keys
   * 6) Number of rejected connections
   * 7) Latest fork(2) time
   * 8) The aof_delayed_fsync counter
   *
   * @return always OK.
   */
  String configResetStat();

  /**
   * Rewrites the redis.conf file the server was started with, applying
   * the minimal changes needed to make it reflect the configuration
   * currently used by the server, which may be different compared to the
   * original one because of the use of the CONFIG SET command.
   *
   * @return OK when the configuration was rewritten properly.
   * Otherwise, an error is returned.
   */
  String configRewrite();

}
