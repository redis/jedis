package redis.clients.jedis;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.BeforeClass;
import org.junit.Test;

public class SSLJedisSentinelPoolTest {

  private static final String MASTER_NAME = "aclmaster";

  private static Set<HostAndPort> sentinels = new HashSet<>();

  // don't map IP addresses so that we try to connect with host 127.0.0.1
  private final HostAndPortMapper SSL_PORT_MAPPER = (HostAndPort hostAndPort) -> {
    if (hostAndPort.getHost().startsWith("172")) {
      return mapClusterAddress(hostAndPort.getHost(), hostAndPort.getPort());
    }
    return new HostAndPort(hostAndPort.getHost(), hostAndPort.getPort() + 10000);
  };
  private static final GenericObjectPoolConfig<Jedis> POOL_CONFIG = new GenericObjectPoolConfig<>();

  private static HostAndPort mapClusterAddress(String host, int port) {
    String[] segments = host.split("\\.");
    if (segments.length == 4) {
      int lastSegment = Integer.parseInt(segments[3]);
      host = "127.0.0.1";
      if (lastSegment < 30) {
        int delta = lastSegment - 10; // 172.21.0.10 is the first IP for non-sentinels
        port = 6379 + delta + 10000; // stunnel serves non-sentinels on 16379...
      } else {
        int delta = lastSegment - 31; // 172.21.0.31 is the first IP for sentinels
        port = 26379 + delta + 10000; // stunnel serves sentinels on 36379...
      }
    }
    return new HostAndPort(host, port);
  }


  @BeforeClass
  public static void prepare() {
    SSLJedisTest.setupTrustStore();

    sentinels.add(HostAndPorts.getSentinelServers().get(4));
  }

  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {

    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .user("acljedis").password("fizzbuzz").clientName("master-client").ssl(true)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user("sentinel").password("foobared").clientName("sentinel-client").ssl(false).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithoutSsl() {

    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .user("acljedis").password("fizzbuzz").clientName("master-client").ssl(false).build();

    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user("sentinel").password("foobared").clientName("sentinel-client")
        .ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithSsl() {

    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .user("acljedis").password("fizzbuzz").clientName("master-client").ssl(true)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user("sentinel").password("foobared").clientName("sentinel-client")
        .ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

}
