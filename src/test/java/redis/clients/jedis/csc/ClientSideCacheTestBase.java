package redis.clients.jedis.csc;

import java.util.function.Supplier;

import io.redis.test.annotations.SinceRedisVersion;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.*;
import redis.clients.jedis.util.RedisVersionCondition;

@SinceRedisVersion(value = "7.4.0", message = "Jedis client-side caching is only supported with Redis 7.4 or later.")
@Tag("integration")
public abstract class ClientSideCacheTestBase {

  protected static EndpointConfig endpoint;

  protected static HostAndPort hnp;

  protected Jedis control;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone1"));

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("standalone1");
    hnp = endpoint.getHostAndPort();
  }

  @BeforeEach
  public void setUp() throws Exception {
    control = new Jedis(hnp, endpoint.getClientConfigBuilder().build());
    control.flushAll();
  }

  @AfterEach
  public void tearDown() throws Exception {
    control.close();
  }

  protected static final Supplier<JedisClientConfig> clientConfig = () -> endpoint.getClientConfigBuilder().resp3().build();

  protected static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig = () -> {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);
    return poolConfig;
  };

}
