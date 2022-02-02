package redis.clients.jedis.commands;

import redis.clients.jedis.Response;

import java.util.List;

public interface ConfigBinaryPipelineCommands {

  /**
   * Used to read the configuration parameters of Redis server.
   *
   * @param pattern name of Redis server's configuration
   * @return value of Redis server's configuration
   */
  Response<List<byte[]>> configGet(byte[] pattern);

  /**
   * Used to read the configuration parameters of Redis server.
   *
   * @param patterns names of Redis server's configuration
   * @return values of Redis server's configuration
   */
  Response<List<byte[]>> configGet(byte[]... patterns);

  /**
   * Used in order to reconfigure the Redis server at run time without
   * the need to restart.
   *
   * @param parameter name of Redis server's configuration
   * @param value     value of Redis server's configuration
   * @return OK when the configuration was set properly.
   * Otherwise, an error is returned.
   */
  Response<String> configSet(byte[] parameter, byte[] value);
}
