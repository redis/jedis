package redis.clients.jedis;

public interface JedisClientConfig extends JedisSocketConfig {

  int getInfiniteSoTimeout();

  String getUser();

  String getPassword();

  int getDatabase();

  String getClientName();
}
