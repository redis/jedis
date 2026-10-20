package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.ConnectionTestHelper.getSocketSoTimeout;
import static redis.clients.jedis.ConnectionTestHelper.isRelaxedTimeoutActive;

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
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));

      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a.ping());

      assertTrue(isRelaxedTimeoutActive(a));
      assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(a));
    }
  }

  /** Cases B2, B3 (section B) in ClusterMaintenanceIT.md. */
  @Test
  public void relaxationIsClientWideAndACloserOnAnyConnectionRevertsAll() throws SocketException {
    openDefaultClient();
    deliver(serverA, A_FROM, migrating(1L, "0-100"));

    try (Connection b = client.getConnectionFromSlot(B_FROM)) {
      assertTrue(b.ping());
      assertTrue(isRelaxedTimeoutActive(b), "B never received a push: the gate is client-wide");
      assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(b));

      // the closer arrives on B only
      serverB.sendPushMessageToAll(neutralCloser(2L));
      assertTrue(b.ping());
      assertFalse(isRelaxedTimeoutActive(b));
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(b));
    }
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(a.ping());
      assertFalse(isRelaxedTimeoutActive(a), "A reverts without having seen the closer itself");
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));
    }
  }

  /** Case B4, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void copiesOfOnePairOnSeveralConnectionsOpenAndCloseOnce() throws SocketException {
    openDefaultClient();
    Connection a1 = client.getConnectionFromSlot(A_FROM);
    Connection a2 = client.getConnectionFromSlot(A_FROM);
    Connection a3 = client.getConnectionFromSlot(A_FROM);
    List<Connection> all = Arrays.asList(a1, a2, a3);
    try {
      // two overlapping migrations; every push below arrives as one copy per connection
      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a1.ping());
      assertTrue(isRelaxedTimeoutActive(a2),
        "the window opened by the copy read on a1 did not relax a2: the gate is not client-wide");
      serverA.sendPushMessageToAll(migrating(3L, "200-300"));
      for (Connection a : all) {
        assertTrue(a.ping());
        assertTrue(isRelaxedTimeoutActive(a));
        assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(a));
      }

      // a closer concludes exactly one window; the second window is what makes a duplicate copy
      // observable: were it applied again, it would conclude that one too and unrelax everything
      serverA.sendPushMessageToAll(neutralCloser(5L));
      assertTrue(a1.ping());
      assertTrue(isRelaxedTimeoutActive(a1), "the first copy of the closer concluded both windows");
      for (Connection a : Arrays.asList(a2, a3)) {
        assertTrue(a.ping());
        assertTrue(isRelaxedTimeoutActive(a),
          "a later copy of the same closer concluded the remaining window");
        assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(a));
      }

      // a real server does not deliver the same closer twice; this exercises idempotence: the
      // remaining window has a lower seq, so a re-applied copy would conclude it
      serverA.sendPushMessageToAll(neutralCloser(5L));
      assertTrue(a2.ping());
      assertTrue(isRelaxedTimeoutActive(a2),
        "a re-delivered closer concluded the remaining window although it was already applied");
      for (Connection a : Arrays.asList(a1, a3)) {
        assertTrue(a.ping());
        assertTrue(isRelaxedTimeoutActive(a),
          "a re-delivered copy of the closer concluded the remaining window");
        assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(a));
      }

      // a distinct closer concludes the remaining window
      serverA.sendPushMessageToAll(neutralCloser(6L));
      assertTrue(a1.ping());
      assertFalse(isRelaxedTimeoutActive(a2),
        "a2 is still relaxed after the closer read on a1 concluded the last window");
      for (Connection a : all) {
        assertTrue(a.ping());
        assertFalse(isRelaxedTimeoutActive(a));
        assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));
      }

      // copies of an opener already seen do not reopen its closed window
      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      for (Connection a : all) {
        assertTrue(a.ping());
        assertFalse(isRelaxedTimeoutActive(a),
          "a re-delivered copy of an already seen opener reopened its closed window");
        assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));
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
      assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(a));

      now.addAndGet(TimeUnit.SECONDS.toNanos(3));
      assertTrue(a.ping());

      assertFalse(isRelaxedTimeoutActive(a));
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));
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
      assertTrue(isRelaxedTimeoutActive(a), "the window is open...");
      assertEquals(largeSoTimeoutMs, getSocketSoTimeout(a),
        "...but the looser configured value wins");
    }
  }

  /** Case B7, section B, in ClusterMaintenanceIT.md. */
  @Test
  public void relaxationKeepsAnInfiniteConfiguredTimeout() throws SocketException {
    openClient(clientConfig(RedisProtocol.RESP3, 0), maintConfig(Mode.AUTO));
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(migrating(1L, "0-100"));
      assertTrue(a.ping());
      assertTrue(isRelaxedTimeoutActive(a));
      assertEquals(0, getSocketSoTimeout(a), "infinite is the loosest; it stays");
    }
  }
}
