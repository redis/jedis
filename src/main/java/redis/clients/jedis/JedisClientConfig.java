package redis.clients.jedis;

public interface JedisClientConfig {

  int getInfiniteSoTimeout();

  String getUser();

  String getPassword();

  int getDatabase();

  String getClientName();
}
