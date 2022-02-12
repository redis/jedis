package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

/**
 * The interface about Redis management command
 */
public interface ControlCommands extends AccessControlLogCommands, ClientCommands {

  /**
   * Provide information on the role of a Redis instance in the context of replication,
   * by returning if the instance is currently a master, slave, or sentinel. The command
   * also returns additional information about the state of the replication
   * (if the role is master or slave) or the list of monitored master names (if the role is sentinel).
   *
   * @return The information on the role of a Redis instance
   */
  List<Object> role();

  /**
   * Returns the reference count of the stored at {@code key}.
   *
   * @param key The key in Redis server
   * @return The reference count of the stored at {@code key}
   */
  Long objectRefcount(String key);

  /**
   * Returns the internal encoding for the Redis object stored at {@code key}.
   * <p>
   * See for details: <a href="https://redis.io/commands/object-encoding">OBJECT ENCODING key</a>
   *
   * @param key The key in Redis server
   * @return The number of references
   */
  String objectEncoding(String key);

  /**
   * Returns the time in seconds since the last access to the value stored at {@code key}.
   * The command is only available when the maxmemory-policy configuration directive
   * is not set to one of the LFU policies.
   *
   * @param key The key in Redis server
   * @return The idle time in seconds
   */
  Long objectIdletime(String key);

  /**
   * Returns the object subcommands and usages.
   *
   * @return object subcommands and usages
   */
  List<String> objectHelp();

  /**
   * Returns the logarithmic access frequency counter of a Redis object stored at {@code key}.
   * <p>
   * The command is only available when the maxmemory-policy configuration directive is
   * set to one of the LFU policies.
   *
   * @param key The key in Redis server
   * @return The counter's value
   */
  Long objectFreq(String key);

  /**
   * Reports about different memory-related issues that the Redis server experiences,
   * and advises about possible remedies.
   */
  String memoryDoctor();

  /**
   * Reports the number of bytes that a key and its value require to be stored in RAM.
   * The reported usage is the total of memory allocations for data and administrative
   * overheads that a key its value require.
   * <p>
   * See for details: <a href="https://redis.io/commands/memory-usage">MEMORY USAGE key</a>
   *
   * @param key The key in Redis server
   * @return The memory usage in bytes, or {@code nil} when the key does not exist
   */
  Long memoryUsage(String key);

  /**
   * Reports the number of bytes that a key and its value require to be stored in RAM.
   * The reported usage is the total of memory allocations for data and administrative
   * overheads that a key its value require.
   * <p>
   * See for details: <a href="https://redis.io/commands/memory-usage">MEMORY USAGE key SAMPLES count</a>
   *
   * @param key The key in Redis server
   * @return The memory usage in bytes, or {@code nil} when the key does not exist
   */
  Long memoryUsage(String key, int samples);

  /**
   * Attempts to purge dirty pages so these can be reclaimed by the allocator.
   *
   * @return OK
   */
  String memoryPurge();

  /**
   * Returns an Array reply about the memory usage of the server.
   *
   * @return nested list of memory usage metrics and their values
   */
  Map<String, Object> memoryStats();

}
