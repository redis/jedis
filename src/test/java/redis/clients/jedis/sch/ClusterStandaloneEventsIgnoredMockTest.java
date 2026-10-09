package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.util.server.MaintenanceEventMessages;

/**
 * Section D (standalone / enterprise family on a cluster connection) of the cluster maintenance
 * test plan, against a three-node {@code TcpMockServer} cluster. The cases are defined in
 * ClusterMaintenanceIT.md, in this package directory.
 */
public class ClusterStandaloneEventsIgnoredMockTest extends AbstractClusterMaintenanceMockTest {

  /** Case D1, section D, in ClusterMaintenanceIT.md. */
  @Test
  public void standaloneFamilyPushesAreInertOnClusterConnections() throws SocketException {
    openDefaultClient();
    String key = keyOnA();
    List<String> shards = Collections.singletonList("1");

    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(MaintenanceEventMessages.moving(1, 30, nodeB.toString()));
      serverA.sendPushMessageToAll(MaintenanceEventMessages.migrating(2, 5, shards));
      serverA.sendPushMessageToAll(MaintenanceEventMessages.migrated(3, shards));
      serverA.sendPushMessageToAll(MaintenanceEventMessages.failingOver(4, 5, shards));
      serverA.sendPushMessageToAll(MaintenanceEventMessages.failedOver(5, shards));

      assertTrue(a.ping(), "all five frames consumed, the reply is aligned");
      assertFalse(relaxed(a), "no relaxation from the standalone family");
      assertEquals(SO_TIMEOUT_MS, soTimeout(a));
      assertFalse(ConnectionTestHelper.isRetired(a), "no retirement from MOVING");
    }

    client.set(key, "v");
    assertEquals(1, handlerA.commands("SET").size());
    assertEquals(2, client.getClusterNodes().size(), "topology unchanged");
    assertEquals(1, handlerA.clusterSlotsCalls(), "no refresh");

    // a fresh connection is not remapped to the MOVING target either
    serverA.disconnectAllClients();
    awaitConnections(serverA, 0);
    client.set(key, "v2");
    assertEquals(2, handlerA.commands("SET").size());
    assertEquals(0, serverB.getConnectedClientCount(), "B was never contacted");
  }
}
