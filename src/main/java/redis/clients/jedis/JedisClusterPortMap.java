package redis.clients.jedis;

public interface JedisClusterPortMap {
  int getSSLPort(int port);
}
