package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class NetUtilsTest {

  private static SocketAddress addr(String host) {
    return new InetSocketAddress(host, 6379);
  }

  @Test
  public void privateIpv4RangesArePrivate() {
    assertTrue(NetUtils.isPrivateIp(addr("10.0.0.1")));
    assertTrue(NetUtils.isPrivateIp(addr("172.16.0.1")));
    assertTrue(NetUtils.isPrivateIp(addr("192.168.1.1")));
  }

  @Test
  public void loopbackAndLinkLocalArePrivate() {
    assertTrue(NetUtils.isPrivateIp(addr("127.0.0.1")));
    assertTrue(NetUtils.isPrivateIp(addr("169.254.0.1")));
    assertTrue(NetUtils.isPrivateIp(addr("::1")));
    assertTrue(NetUtils.isPrivateIp(addr("fe80::1")));
  }

  @Test
  public void ipv6UniqueLocalIsPrivate() {
    assertTrue(NetUtils.isPrivateIp(addr("fc00::1")));
    assertTrue(NetUtils.isPrivateIp(addr("fd12:3456:789a::1")));
  }

  @Test
  public void publicAddressesAreNotPrivate() {
    assertFalse(NetUtils.isPrivateIp(addr("8.8.8.8")));
    assertFalse(NetUtils.isPrivateIp(addr("2001:4860:4860::8888")));
  }

  @Test
  public void wildcardUnresolvedAndNonInetAreNotPrivate() {
    assertFalse(NetUtils.isPrivateIp(addr("0.0.0.0")));
    assertFalse(NetUtils.isPrivateIp(InetSocketAddress.createUnresolved("intranet.local", 6379)));
    assertFalse(NetUtils.isPrivateIp(null));
    assertFalse(NetUtils.isPrivateIp(new SocketAddress() {
    }));
  }
}