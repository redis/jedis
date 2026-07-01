package redis.clients.jedis.mcf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushConsumer;
import redis.clients.jedis.PushConsumerChainImpl;
import redis.clients.jedis.util.server.TcpMockServer;

public class TrackingConnectionPoolInitTest {

  private TcpMockServer mockServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  /**
   * Regression test for the double-initialization bug in {@link TrackingConnectionPool}.
   * <p>
   * Before the fix, a {@link Connection} obtained through a {@code TrackingConnectionPool} was
   * initialized twice.
   * </p>
   */
  @Test
  public void pooledConnectionRegistersPubSubConsumerExactlyOnce() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());

    try (
        TrackingConnectionPool pool = TrackingConnectionPool.builder().hostAndPort(hostAndPort)
            .clientConfig(config)
            .maintenanceNotificationsConfig(MaintenanceNotificationsConfig.builder()
                .mode(MaintenanceNotificationsConfig.Mode.ENABLED).build())
            .build();
        Connection conn = pool.getResource()) {

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);
      assertThat(consumers,
        contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER), matchesMaintenanceConsumer()));
    }
  }

  private static Matcher<PushConsumer> matchesMaintenanceConsumer() {
    return new TypeSafeMatcher<PushConsumer>() {
      @Override
      protected boolean matchesSafely(PushConsumer consumer) {
        return ConnectionTestHelper.isMaintenanceEventConsumer(consumer);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a maintenance event consumer");
      }
    };
  }

  /**
   * Regression test for the double-initialization bug in {@link TrackingConnectionPool}.
   * <p>
   * Double initialization re-runs the full handshake over the same socket, so the server would see
   * {@code HELLO} twice. Counting it at the mock server asserts the handshake runs exactly once per
   * borrowed connection.
   * </p>
   */
  @Test
  public void pooledConnectionPerformsHandshakeExactlyOnce() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();
    HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());

    AtomicInteger helloCount = new AtomicInteger();
    mockServer.setCommandHandler((commandArgs, clientId) -> {
      if (commandArgs.getCommand() == Protocol.Command.HELLO) {
        helloCount.incrementAndGet();
      }
      return null; // fall back to built-in responses (incl. the HELLO reply)
    });

    try (
        TrackingConnectionPool pool = TrackingConnectionPool.builder().hostAndPort(hostAndPort)
            .clientConfig(config).build();
        Connection conn = pool.getResource()) {

      assertEquals(1, helloCount.get(),
        "handshake (HELLO) must run exactly once per borrowed connection");
    }
  }

}
