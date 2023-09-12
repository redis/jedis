package redis.clients.jedis;

/**
 * This interface is to modify the behavior of internally executing CLIENT SETINFO command.
 */
public interface ClientSetInfoConfig {

  default boolean isDisable() {
    return false;
  }

  default String getLibNameSuffix() {
    return null;
  }
}
