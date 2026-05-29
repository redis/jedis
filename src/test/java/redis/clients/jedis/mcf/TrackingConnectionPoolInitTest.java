package redis.clients.jedis.mcf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
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
            .clientConfig(config).build();
        Connection conn = pool.getResource()) {

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);
      // contains(...) is an exact-length matcher: two copies of PUBSUB_CONSUMER would fail.
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
    }
  }
}
