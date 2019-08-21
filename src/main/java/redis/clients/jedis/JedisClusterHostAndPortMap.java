package redis.clients.jedis;

public interface JedisClusterHostAndPortMap {
  HostAndPort getSSLHostAndPort(String host, int port);
}
