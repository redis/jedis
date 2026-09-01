package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointType;
import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointTypeResolver;

@Tag("unit")
public class AutoEndpointTypeResolverTest {

  private static final EndpointTypeResolver RESOLVER = MaintenanceNotificationsConfig.builder()
      .build().getEndpointTypeResolver();

  private static SocketAddress addr(String host) {
    return new InetSocketAddress(host, 6379);
  }

  private static EndpointType resolve(SocketAddress address) {
    return RESOLVER.getEndpointType(address, false);
  }

  @Test
  public void privateIpv4RangesResolveInternal() {
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("10.0.0.1")));
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("172.16.0.1")));
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("192.168.1.1")));
  }

  @Test
  public void loopbackAndLinkLocalResolveInternal() {
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("127.0.0.1")));
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("169.254.0.1")));
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("::1")));
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("fe80::1")));
  }

  @Test
  public void ipv6UniqueLocalResolvesInternal() {
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("fc00::1")));
    assertEquals(EndpointType.INTERNAL_IP, resolve(addr("fd12:3456:789a::1")));
  }

  @Test
  public void publicAddressesResolveExternal() {
    assertEquals(EndpointType.EXTERNAL_IP, resolve(addr("8.8.8.8")));
    assertEquals(EndpointType.EXTERNAL_IP, resolve(addr("2001:4860:4860::8888")));
  }

  @Test
  public void wildcardUnresolvedAndNonInetResolveExternal() {
    assertEquals(EndpointType.EXTERNAL_IP, resolve(addr("0.0.0.0")));
    assertEquals(EndpointType.EXTERNAL_IP,
      resolve(InetSocketAddress.createUnresolved("intranet.local", 6379)));
    assertEquals(EndpointType.EXTERNAL_IP, resolve(null));
    assertEquals(EndpointType.EXTERNAL_IP, resolve(new SocketAddress() {
    }));
  }

  @Test
  public void tlsSelectsFqdnVariants() {
    assertEquals(EndpointType.INTERNAL_FQDN, RESOLVER.getEndpointType(addr("10.0.0.1"), true));
    assertEquals(EndpointType.EXTERNAL_FQDN, RESOLVER.getEndpointType(addr("8.8.8.8"), true));
  }
}
