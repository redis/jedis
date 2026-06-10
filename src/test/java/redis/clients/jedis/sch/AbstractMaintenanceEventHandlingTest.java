package redis.clients.jedis.sch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static redis.clients.jedis.sch.MaintenanceEventTestSupport.enabledMaintConfig;
import static redis.clients.jedis.sch.MaintenanceEventTestSupport.isMaintenanceConsumer;
import static redis.clients.jedis.sch.MaintenanceEventTestSupport.isPubSubConsumer;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Shared behavior tests for maintenance push-consumer registration. Concrete subclasses provide the
 * connection-construction hooks for the variant under test (plain {@link Connection},
 * {@link redis.clients.jedis.csc.CacheConnection}, or other future variants).
 */
public abstract class AbstractMaintenanceEventHandlingTest {

  protected TcpMockServer mockServer;

  @BeforeEach
  public void schSetUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
  }

  @AfterEach
  public void schTearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  /** Build the pool used to borrow the connection under test. */
  protected abstract ConnectionPool createPool(HostAndPort hostAndPort, JedisClientConfig config);

  /** Build a connection via a public constructor (no pool, no injected controller). */
  protected abstract Connection buildDirect(HostAndPort hostAndPort, JedisClientConfig config);

  /** Build a connection via {@code .builder()} (no pool, no injected controller). */
  protected abstract Connection buildFromBuilder(DefaultJedisSocketFactory socketFactory,
      JedisClientConfig config);

  // ---- Tests ---------------------------------------------------------------

  /** A pooled connection registers the maintenance consumer alongside the pub/sub consumer. */
  @Test
  public void maintenanceConsumerRegisteredForPooledConnection() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(enabledMaintConfig()).build();

    try (ConnectionPool pool = createPool(new HostAndPort("localhost", mockServer.getPort()),
      config)) {
      Connection conn = pool.getResource();
      try {
        assertThat(ConnectionTestHelper.getPushConsumers(conn), hasItem(isPubSubConsumer()));
        assertThat(ConnectionTestHelper.getPushConsumers(conn), hasItem(isMaintenanceConsumer()));
      } finally {
        conn.close();
      }
    }
  }

  /** A non-pooled (direct-constructor) connection has no controller — no maintenance consumer. */
  @Test
  public void maintenanceConsumerNotRegisteredForNonPooledConnection() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(enabledMaintConfig()).build();

    try (Connection conn = buildDirect(new HostAndPort("localhost", mockServer.getPort()), config)) {
      assertThat(ConnectionTestHelper.getPushConsumers(conn), hasItem(isPubSubConsumer()));
      assertThat(ConnectionTestHelper.getPushConsumers(conn),
        not(hasItem(isMaintenanceConsumer())));
    }
  }

  /** A builder-built connection (no injected controller) is non-pooled: no maintenance consumer. */
  @Test
  public void maintenanceConsumerNotRegisteredForConnectionBuilder() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(enabledMaintConfig()).build();
    DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(
        new HostAndPort("localhost", mockServer.getPort()), config);

    try (Connection conn = buildFromBuilder(socketFactory, config)) {
      assertThat(ConnectionTestHelper.getPushConsumers(conn),
        not(hasItem(isMaintenanceConsumer())));
    }
  }

  /**
   * Maintenance config left at the default (no explicit timeout options) on a direct
   * (non-pooled) connection — still no maintenance consumer because no controller is wired.
   */
  @Test
  public void maintenanceConsumerNotRegisteredWithDefaultConfigOnDirectConnection() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().build();

    try (Connection conn = buildDirect(new HostAndPort("localhost", mockServer.getPort()), config)) {
      assertThat(ConnectionTestHelper.getPushConsumers(conn), hasItem(isPubSubConsumer()));
      assertThat(ConnectionTestHelper.getPushConsumers(conn),
        not(hasItem(isMaintenanceConsumer())));
    }
  }
}
