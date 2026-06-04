package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.pool2.PooledObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RebindAware.RebindResult;
import redis.clients.jedis.util.ReflectionTestUtil;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for the lock-free, sequence-guarded, time-bounded MOVING rebind overlay owned by
 * {@link ConnectionFactory}. The socket factory resolves its target through the injected supplier;
 * an injected monotonic clock makes expiry deterministic.
 */
@Tag("sch")
public class ConnectionFactoryRebindTest {

  private static final HostAndPort ORIGINAL = new HostAndPort("original.example.com", 6379);
  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);
  private static final HostAndPort TARGET_C = new HostAndPort("node-c.example.com", 6381);

  private DefaultJedisSocketFactory socketFactory;
  private ConnectionFactory connectionFactory;
  private AtomicLong now;

  @BeforeEach
  public void setUp() {
    socketFactory = new DefaultJedisSocketFactory(ORIGINAL);
    connectionFactory = new ConnectionFactory(socketFactory,
        DefaultJedisClientConfig.builder().build());
    now = new AtomicLong(0);
    connectionFactory.setClockNanos(now::get);
  }

  private void advanceSeconds(long seconds) {
    now.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  @Test
  public void noRebind_usesOriginal() {
    assertEquals(ORIGINAL, socketFactory.getHostAndPort());
  }

  @Test
  public void applyNewTarget_thenExpiresBackToOriginal() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(1L, TARGET_B, 10));
    assertEquals(TARGET_B, socketFactory.getHostAndPort(), "within grace window uses new target");

    advanceSeconds(9);
    assertEquals(TARGET_B, socketFactory.getHostAndPort(), "still within window");

    advanceSeconds(2); // now past the 10s deadline
    assertEquals(ORIGINAL, socketFactory.getHostAndPort(), "after expiry reverts to original");
  }

  @Test
  public void staleAndOutOfOrderEvents_areIgnored() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(5L, TARGET_B, 100));

    assertEquals(RebindResult.STALE, connectionFactory.rebind(5L, TARGET_C, 100),
      "same seq is a duplicate");
    assertEquals(RebindResult.STALE, connectionFactory.rebind(3L, TARGET_C, 100),
      "lower seq is out-of-order");
    assertEquals(TARGET_B, socketFactory.getHostAndPort(), "target unchanged by stale events");
  }

  @Test
  public void higherSeqSameTarget_isApplied() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(5L, TARGET_B, 10));
    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(6L, TARGET_B, 10),
      "a strictly newer seq is always applied; dedup is by seq, not by target");
    assertEquals(TARGET_B, socketFactory.getHostAndPort());
  }

  @Test
  public void higherSeqNewTarget_supersedes() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(5L, TARGET_B, 100));
    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(6L, TARGET_C, 100));
    assertEquals(TARGET_C, socketFactory.getHostAndPort());
  }

  @Test
  public void newEventAfterExpiry_appliesAgain() {
    connectionFactory.rebind(5L, TARGET_B, 10);
    advanceSeconds(11); // expire
    assertEquals(ORIGINAL, socketFactory.getHostAndPort());

    assertEquals(RebindResult.APPLIED_NEW_TARGET, connectionFactory.rebind(6L, TARGET_C, 10));
    assertEquals(TARGET_C, socketFactory.getHostAndPort(),
      "a newer event after expiry applies again");
  }

  @Test
  public void borrowRelaxesConnectionDuringRebindWindow() throws Exception {
    int soTimeoutMs = 2000;
    int relaxedTimeoutMs = 10000;
    TcpMockServer mockServer = new TcpMockServer();
    mockServer.start();
    try {
      MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
          .timeoutOptions(TimeoutOptions.builder()
              .proactiveTimeoutsRelaxing(Duration.ofMillis(relaxedTimeoutMs)).build())
          .build();
      DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
          .socketTimeoutMillis(soTimeoutMs).maintNotificationsConfig(maintConfig)
          .protocol(RedisProtocol.RESP3).build();
      HostAndPort mock = new HostAndPort("localhost", mockServer.getPort());

      DefaultJedisSocketFactory mockSocketFactory = new DefaultJedisSocketFactory(mock,
          clientConfig);
      ConnectionFactory factory = new ConnectionFactory(mockSocketFactory, clientConfig);

      assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(1L, mock, 100));
      PooledObject<Connection> pooled = factory.makeObject();
      try {
        factory.activateObject(pooled); // simulate a pool borrow
        Connection borrowed = pooled.getObject();
        assertTrue(borrowed.isRelaxedTimeoutActive(),
          "a connection borrowed during a rebind window is relaxed");
        Socket socket = ReflectionTestUtil.getField(borrowed, "socket");
        assertEquals(relaxedTimeoutMs, socket.getSoTimeout());
      } finally {
        pooled.getObject().close();
      }
    } finally {
      mockServer.stop();
    }
  }
}
