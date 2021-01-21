package redis.clients.jedis;

/**
 * @deprecated Use HostAndPortMapper
 */
@Deprecated
public interface JedisClusterHostAndPortMap extends HostAndPortMapper {

  HostAndPort getSSLHostAndPort(String host, int port);

  @Override
  default HostAndPort getHostAndPort(HostAndPort hap) {
    return getSSLHostAndPort(hap.getHost(), hap.getPort());
  }
}
