package redis.clients.jedis;

public interface HostAndPortMapper {

  HostAndPort getHostAndPort(HostAndPort hap);
}
