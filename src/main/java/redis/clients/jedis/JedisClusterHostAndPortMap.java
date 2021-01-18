package redis.clients.jedis;

/**
 * @deprecated Use HostAndPortMapper
 */
public interface JedisClusterHostAndPortMap extends HostAndPortMapper {

  HostAndPort getSSLHostAndPort(String host, int port);

  @Override
  default HostAndPort getHostAndPort(String host, int port) {
    return getSSLHostAndPort(host, port);
  }

  @Override
  default HostAndPort getHostAndPort(HostAndPort hap) {
    return getHostAndPort(hap.getHost(), hap.getPort());
  }
}
