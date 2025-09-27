package redis.clients.jedis;

/**
 * An interface for mapping Redis node addresses.
 * <p>
 * It is used to translate an advertised server address to one that is reachable by the client,
 * especially in network topologies involving NAT or containerization.
 */
@FunctionalInterface
public interface HostAndPortMapper {

  /**
   * @param hap The original address from the server.
   * @return The translated, reachable address.
   */
  HostAndPort getHostAndPort(HostAndPort hap);
}
