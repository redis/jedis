package redis.clients.jedis;

/**
 * This interface is to modify the behavior of internally executing CLIENT SETINFO command.
 */
public interface ClientSetInfoConfig {

  default boolean isDisabled() {
    return false;
  }

  /**
   * If provided, this suffix will be enclosed by braces {@code ()}.
   */
  default String getLibNameSuffix() {
    return null;
  }
}
