package redis.clients.jedis;

/**
 * @deprecated This interface will be removed in next major release. Use {@link HostAndPortMapper}.
 */
@Deprecated
public interface JedisClusterHostAndPortMap extends HostAndPortMapper {

  HostAndPort getSSLHostAndPort(String host, int port);

  @Override
  default HostAndPort getHostAndPort(HostAndPort hap) {
    return getSSLHostAndPort(hap.getHost(), hap.getPort());
  }
}
