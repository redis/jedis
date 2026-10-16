package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

/**
 * Section B (client-wide relaxed timeouts) of the cluster maintenance test plan, against a
 * three-node {@code TcpMockServer} cluster. Baseline socket timeout 2000 ms (blocking 1000 ms),
 * relaxed 10000 ms (blocking 4000 ms). The cases are defined in ClusterMaintenanceIT.md, in this
 * package directory.
 */
public class ClusterRelaxedTimeoutMockTest extends AbstractClusterMaintenanceMockTest {

  /** Case B1, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void sMigratingRelaxesTheReceivingConnection() throws SocketException {
    openDefaultClient();
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(a.ping());
      assertEquals(SO_TIMEOUT_MS, soTimeout(a));

      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a.ping());

      assertTrue(relaxed(a));
      assertEquals(RELAXED_TIMEOUT_MS, soTimeout(a));
    }
  }

  /** Cases B2, B3 (section B) in ClusterMaintenanceIT.md. */
  @Test
  public void relaxationIsClientWideAndACloserOnAnyConnectionRevertsAll() throws SocketException {
    openDefaultClient();
    deliver(serverA, A_FROM, migrating(1L, "0-100"));

    try (Connection b = client.getConnectionFromSlot(B_FROM)) {
      assertTrue(b.ping());
      assertTrue(relaxed(b), "B never received a push: the gate is client-wide");
      assertEquals(RELAXED_TIMEOUT_MS, soTimeout(b));

      // the closer arrives on B only
      serverB.sendPushMessageToAll(neutralCloser(2L));
      assertTrue(b.ping());
      assertFalse(relaxed(b));
      assertEquals(SO_TIMEOUT_MS, soTimeout(b));
    }
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(a.ping());
      assertFalse(relaxed(a), "A reverts without having seen the closer itself");
      assertEquals(SO_TIMEOUT_MS, soTimeout(a));
    }
  }

  /** Case B4, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void copiesOfOnePairOnSeveralConnectionsOpenAndCloseOnce() throws SocketException {
    openDefaultClient();
    Connection a1 = client.getConnectionFromSlot(A_FROM);
    Connection a2 = client.getConnectionFromSlot(A_FROM);
    Connection a3 = client.getConnectionFromSlot(A_FROM);
    try {
      serverA.sendPushMessageToAll(migrating(1L, "0-100")); // one copy per connection
      for (Connection a : Arrays.asList(a1, a2, a3)) {
        assertTrue(a.ping());
        assertTrue(relaxed(a));
        assertEquals(RELAXED_TIMEOUT_MS, soTimeout(a));
      }

      serverA.sendPushMessageToAll(neutralCloser(2L)); // one copy per connection
      assertTrue(a1.ping());
      assertFalse(relaxed(a1), "the first copy closes the window");
      for (Connection a : Arrays.asList(a2, a3)) {
        assertTrue(a.ping());
        assertFalse(relaxed(a), "later copies neither reopen nor fail");
        assertEquals(SO_TIMEOUT_MS, soTimeout(a));
      }
    } finally {
      a1.close();
      a2.close();
      a3.close();
    }
    client.set(keyOnA(), "v");
    assertTrue(hasNode(nodeA));
  }

  /** Case B5, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void windowWithALostCloserRevertsAtTheTtlBackstop() throws SocketException {
    AtomicLong now = new AtomicLong(System.nanoTime());
    ConnectionTestHelper.setClockNanos(now::get);
    client = clientBuilder(defaultClientConfig(),
      maintConfigBuilder(Mode.AUTO).relaxedWindowMaxDuration(Duration.ofSeconds(2)).build())
          .build();

    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a.ping());
      assertEquals(RELAXED_TIMEOUT_MS, soTimeout(a));

      now.addAndGet(TimeUnit.SECONDS.toNanos(3));
      assertTrue(a.ping());

      assertFalse(relaxed(a));
      assertEquals(SO_TIMEOUT_MS, soTimeout(a));
    }
  }

  /** Case B6, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void relaxedBlockingTimeoutAppliesDuringABlockingCommand() {
    handlerA.blockingReplyDelayMillis(2000); // > 1000 configured blocking, < 4000 relaxed blocking
    client = clientBuilder(defaultClientConfig(), maintConfig(Mode.AUTO)).maxAttempts(1).build();
    String key = keyOnA();
    deliver(serverA, A_FROM, migrating(1L, "0-100"));

    List<String> popped = client.blpop(0, key);
    assertEquals(Arrays.asList(key, "popped"), popped);

    // window closed: the same server delay now exceeds the configured blocking timeout
    deliver(serverA, A_FROM, neutralCloser(2L));
    assertThrows(JedisClusterOperationException.class, () -> client.blpop(0, key));
  }

  /** Case B7, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void relaxationNeverTightensALargerConfiguredTimeout() throws SocketException {
    int largeSoTimeoutMs = 20_000;
    openClient(clientConfig(RedisProtocol.RESP3, largeSoTimeoutMs), maintConfig(Mode.AUTO));
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a.ping());
      assertTrue(relaxed(a), "the window is open...");
      assertEquals(largeSoTimeoutMs, soTimeout(a), "...but the looser configured value wins");
    }
  }

  /** Case B7, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void relaxationKeepsAnInfiniteConfiguredTimeout() throws SocketException {
    openClient(clientConfig(RedisProtocol.RESP3, 0), maintConfig(Mode.AUTO));
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a.ping());
      assertTrue(relaxed(a));
      assertEquals(0, soTimeout(a), "infinite is the loosest; it stays");
    }
  }
}
