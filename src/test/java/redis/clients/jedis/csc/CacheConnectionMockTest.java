package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.PushConsumer;
import redis.clients.jedis.PushConsumerChain;
import redis.server.stub.RedisServerStub;

/**
 * Unit tests for CacheConnection that don't require a real Redis server. Uses TcpMockServer to
 * simulate Redis protocol.
 * <p>
 * These tests verify CacheConnection-specific behavior (PushInvalidateConsumer registration).
 * MaintenanceEventConsumer registration is tested in ConnectionMockTest.
 * </p>
 */
public class CacheConnectionMockTest {

  private RedisServerStub mockServer;
  private Cache cache;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new RedisServerStub();
    mockServer.start();
    cache = CacheFactory.getCache(CacheConfig.builder().maxSize(1000).build());
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Nested
  class PushInvalidateConsumer {

    @Test
    public void pushInvalidateConsumerRegisteredWithConfigConstructor() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      CacheConnection conn = new CacheConnection(socketFactory, config, cache);

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify PushInvalidateConsumer is registered
      assertThat(consumers, contains(is(PushConsumerChain.PUBSUB_ONLY_CONSUMER),
        instanceOf(CacheConnection.PushInvalidateConsumer.class)));
    }

    @Test
    public void pushInvalidateConsumerRegisteredWithBuilder() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      CacheConnection conn = (CacheConnection) CacheConnection.builder(cache)
          .socketFactory(socketFactory).clientConfig(config).build();

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify PushInvalidateConsumer is registered
      assertThat(consumers, contains(is(PushConsumerChain.PUBSUB_ONLY_CONSUMER),
        instanceOf(CacheConnection.PushInvalidateConsumer.class)));
    }

  }
}
