package redis.clients.jedis.commands;

import java.util.List;

//Legacy
public interface JedisClusterScriptingCommands {

  Object eval(String script, int keyCount, String... params);

  Object eval(String script, List<String> keys, List<String> args);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   */
  Object eval(String script, String sampleKey);

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   */
  Object evalsha(String sha1, String sampleKey);

  Object evalsha(String sha1, List<String> keys, List<String> args);

  Object evalsha(String sha1, int keyCount, String... params);

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   */
  Boolean scriptExists(String sha1, String sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   * @param sha1
   */
  List<Boolean> scriptExists(String sampleKey, String... sha1);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   */
  String scriptLoad(String script, String sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   */
  String scriptFlush(String sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   *          assigned to
   */
  String scriptKill(String sampleKey);
}
