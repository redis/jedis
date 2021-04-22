package redis.clients.jedis;

/**
 * @deprecated This will be removed in future. Prefer to use {@link HostAndPortMapper} instead.
 */
@Deprecated
public interface JedisClusterHostAndPortMap extends HostAndPortMapper {

  HostAndPort getSSLHostAndPort(String host, int port);

  @Override
  default HostAndPort getHostAndPort(HostAndPort hap) {
    return getSSLHostAndPort(hap.getHost(), hap.getPort());
  }
}
