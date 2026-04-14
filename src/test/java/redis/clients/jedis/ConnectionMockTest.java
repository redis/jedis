package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.server.stub.RedisServerStub;
import redis.server.stub.RedisServerStubConfig;

/**
 * Unit tests for Connection that don't require a real Redis server. Uses TcpMockServer to simulate
 * Redis protocol.
 */
public class ConnectionMockTest {

  private RedisServerStub mockServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new RedisServerStub(RedisServerStubConfig.builder().build());
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Nested
  class MaintenanceEvents {

    /**
     * Tests that the MaintenanceEventConsumer is not registered when using the constructors not
     * providing a JedisClientConfig.
     */
    @Test
    public void maintenanceConsumerNotRegisteredConstructorWithoutConfig() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChain.PUBSUB_ONLY_CONSUMER)));
    }

    /**
     * Tests that the MaintenanceEventConsumer is registered when using the constructors not
     * providing a JedisClientConfig.
     */
    @Test
    public void maintenanceConsumerRegisteredConstructorWithConfig() {
      TimeoutOptions timeoutOpts = TimeoutOptions.builder()
          .proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build();

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
          .timeoutOptions(timeoutOpts).build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChain.PUBSUB_ONLY_CONSUMER),
        instanceOf(Connection.MaintenanceEventConsumer.class)));
    }

    @Test
    public void maintenanceConsumerRegisteredWithConnectionBuilder() {
      TimeoutOptions timeoutOpts = TimeoutOptions.builder()
          .proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build();

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
          .timeoutOptions(timeoutOpts).build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = Connection.builder().socketFactory(socketFactory).clientConfig(config)
          .build();

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChain.PUBSUB_ONLY_CONSUMER),
        instanceOf(Connection.MaintenanceEventConsumer.class)));
    }

    @Test
    public void maintenanceConsumerNotRegisteredRelaxedTimeoutDisabled() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChain.PUBSUB_ONLY_CONSUMER)));
    }
  }
}
