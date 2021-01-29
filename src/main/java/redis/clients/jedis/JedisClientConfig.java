package redis.clients.jedis;

public interface JedisClientConfig extends JedisSocketConfig {

  /**
   * @return Socket timeout (in milliseconds) to use during blocking operation. Default is '0',
   * which means to block forever.
   */
  int getInfiniteSoTimeout();

  String getUser();

  String getPassword();

  int getDatabase();

  String getClientName();
}
