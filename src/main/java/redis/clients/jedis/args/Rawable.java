package redis.clients.jedis.args;

/**
 * The Interface used to save args with byte array.
 */
public interface Rawable {

  /**
   * Get args with byte array.
   * @return byte array of arg
   */
  byte[] getRaw();
}
