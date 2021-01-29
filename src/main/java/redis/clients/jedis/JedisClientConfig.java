package redis.clients.jedis;

public interface JedisClientConfig extends JedisSocketConfig {

  /**
   * Socket timeout (in milliseconds) to use during blocking operation.
   * Default is '0' which means to block forever.
   * @return 
   */
  int getInfiniteSoTimeout();

  String getUser();

  String getPassword();

  int getDatabase();

  String getClientName();
}
