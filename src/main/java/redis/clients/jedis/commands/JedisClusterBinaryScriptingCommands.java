package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;

import java.util.List;

public interface JedisClusterBinaryScriptingCommands {

  Object eval(byte[] script, byte[] keyCount, byte[]... params);

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   */
  Object eval(byte[] script, byte[] sampleKey);

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   */
  Object evalsha(byte[] sha1, byte[] sampleKey);

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   * @param sha1
   */
  List<Long> scriptExists(byte[] sampleKey, byte[]... sha1);

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   */
  byte[] scriptLoad(byte[] script, byte[] sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   */
  String scriptFlush(byte[] sampleKey);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   * @param flushMode
   */
  String scriptFlush(byte[] sampleKey, FlushMode flushMode);

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is
   * assigned to
   */
  String scriptKill(byte[] sampleKey);
}
