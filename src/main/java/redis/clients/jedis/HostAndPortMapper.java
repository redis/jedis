package redis.clients.jedis;

public interface HostAndPortMapper {

  HostAndPort getHostAndPort(String host, int port);

  HostAndPort getHostAndPort(HostAndPort hap);
}
