package redis.clients.jedis.commands;

import java.util.List;

public interface JedisClusterBinaryScriptingCommands extends BinaryScriptingCommands {

  @Override
  default Object eval(byte[] script) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Object eval(byte[] script, byte[] sampleKey);

  @Override
  default Object evalsha(byte[] sha1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sha1
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  Object evalsha(byte[] sha1, byte[] sampleKey);

  @Override
  default List<Long> scriptExists(byte[]... sha1) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @param sha1
   * @return 
   */
  List<Long> scriptExists(byte[] sampleKey, byte[]... sha1);

  @Override
  default byte[] scriptLoad(byte[] script) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param script
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  byte[] scriptLoad(byte[] script, byte[] sampleKey);

  @Override
  default String scriptFlush() {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptFlush(byte[] sampleKey);

  @Override
  default String scriptKill() {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sampleKey Command will be executed in the node where the hash slot of this key is assigned to
   * @return 
   */
  String scriptKill(byte[] sampleKey);
}
