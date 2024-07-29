package redis.clients.jedis.args;

/**
 * Byte array representation of arguments to write in socket input stream.
 */
public interface Rawable {

  /**
   * Get byte array.
   * @return binary
   */
  byte[] getRaw();

  @Override
  int hashCode();

  @Override
  boolean equals(Object o);
}
