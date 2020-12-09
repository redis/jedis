package redis.clients.jedis.commands;

import java.util.List;

public interface JedisClusterBinaryScriptingCommands {
  Object eval(byte[] script, byte[] keyCount, byte[]... params);

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Object eval(byte[] script, byte[] sampleKey);

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Object evalsha(byte[] sha1, byte[] sampleKey);

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @param sha1
   * @return 
   */
  List<Long> scriptExists(byte[] sampleKey, byte[]... sha1);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  byte[] scriptLoad(byte[] script, byte[] sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptFlush(byte[] sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptKill(byte[] sampleKey);
}
