package redis.clients.jedis.commands;

import java.util.List;

public interface JedisClusterScriptingCommands {
  Object eval(String script, int keyCount, String... params);

  Object eval(String script, List<String> keys, List<String> args);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Object eval(String script, String sampleKey);

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Object evalsha(String sha1, String sampleKey);

  Object evalsha(String sha1, List<String> keys, List<String> args);

  Object evalsha(String sha1, int keyCount, String... params);

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Boolean scriptExists(String sha1, String sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @param sha1
   * @return 
   */
  List<Boolean> scriptExists(String sampleKey, String... sha1);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptLoad(String script, String sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptFlush(String sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptKill(String sampleKey);
}
