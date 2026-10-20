package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.SlotMigrateEffect;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.resps.ClusterShardInfo;
import redis.clients.jedis.resps.ClusterShardNodeInfo;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Whether the Redis Enterprise OSS Cluster API exposes a database's replicas to a cluster client.
 * Jedis learns the topology from {@code CLUSTER SLOTS}
 * ({@code JedisClusterInfoCache.discoverClusterSlots}): in every slot-range entry the first node is
 * the master and every further node a replica, which the client only routes to with
 * {@code readOnlyForRedisClusterReplicas()}. This test provisions a replicated OSS-cluster database
 * (the {@code failover} rows are the only replicated dbconfigs the FI offers), asks every node the
 * client found the same question, and asserts what the server exposes, cross-checked with
 * {@code CLUSTER SHARDS} (role per node), {@code CLUSTER NODES} (flags) and
 * {@code INFO replication} (the proxy's own view of its replicas).
 * <p>
 * Finding on 2026-10-20 (RE with Redis 8.6.2, OSS cluster API,
 * {@code proxy_policy=all-master-shards}, 6 shards replicated): one node entry per range on every
 * node and no replica entries; {@code CLUSTER SHARDS} lists only {@code role=master};
 * {@code CLUSTER NODES} lists only masters; {@code INFO replication} on each node reports
 * {@code role:master} with one connected replica at a placeholder address ({@code 0.0.0.0:0}). The
 * replicas exist but are not addressable through the cluster API, so a
 * {@code readOnlyForRedisClusterReplicas()} client has no replica pools and its replica reads fall
 * back to the master. The assertions pin that down: if RE starts exposing replicas,
 * {@link #replicasAreNotExposedByTheClusterApi} fails and names the entries it saw.
 */
@Tag("scenario")
public class ClusterReplicaExposureIT extends ClusterMaintNotificationsScenarioBase {

  private static final Logger log = LoggerFactory.getLogger(ClusterReplicaExposureIT.class);

  /** A CLUSTER SLOTS range is [start, end, master, replica...]. */
  private static final int MASTER_NODE_INDEX = 2;

  @Test
  @Timeout(300)
  void replicasAreNotExposedByTheClusterApi() {
    List<Scenario> rows = new ArrayList<>();
    ClusterMaintNotificationsIT.row(rows, SlotMigrateEffect.SLOT_SHUFFLE, "failover",
      "external/ip");
    Scenario scenario = rows.get(0);
    Map<String, Object> dbConfig = scenario.requirement().dbConfig();
    assertEquals(Boolean.TRUE, dbConfig.get("replication"),
      scenario + ": the row must be replicated for this test to mean anything");
    int shards = ((Number) dbConfig.get("shards_count")).intValue();

    deployDatabase(scenario);
    openVerificationClient(database);
    Set<String> clientNodes = nodeKeys(verification);
    log.info("{}: {} shards, replication on; the client built pools for {}", scenario, shards,
      clientNodes);

    Observations obs = new Observations();
    Set<String> mastersSeen = new LinkedHashSet<>();
    for (String nodeKey : clientNodes) {
      HostAndPort node = HostAndPort.from(nodeKey);
      try (Jedis jedis = new Jedis(node, clientConfig(database).build())) {
        // the exact command the client's topology discovery sends
        SlotsView slots = SlotsView.parse(jedis.clusterSlots());
        mastersSeen.addAll(slots.masters);
        log.info(
          "{}: CLUSTER SLOTS via {}: {} ranges covering {} slots, masters {}, replica"
              + " entries {}",
          scenario, node, slots.ranges, slots.slotsCovered, slots.masters, slots.replicas);
        obs.check("CLUSTER SLOTS via " + node + " covers every slot",
          () -> assertEquals(Protocol.CLUSTER_HASHSLOTS, slots.slotsCovered, "slots covered"));
        obs.check("CLUSTER SLOTS via " + node + " exposes no replica",
          () -> assertTrue(slots.replicas.isEmpty(), "replica entries exposed: " + slots.replicas));

        List<String> nonMasters = new ArrayList<>();
        List<ClusterShardInfo> shardInfos = jedis.clusterShards();
        for (ClusterShardInfo shard : shardInfos) {
          for (ClusterShardNodeInfo n : shard.getNodes()) {
            if (!"master".equals(n.getRole())) {
              nonMasters.add(n.getRole() + "@" + n.getEndpoint() + ":" + n.getPort());
            }
          }
        }
        log.info("{}: CLUSTER SHARDS via {}: {} shards, non-master nodes {}", scenario, node,
          shardInfos.size(), nonMasters);
        obs.check("CLUSTER SHARDS via " + node + " lists only masters",
          () -> assertTrue(nonMasters.isEmpty(), "non-master nodes: " + nonMasters));

        List<String> replicaLines = new ArrayList<>();
        for (String line : jedis.clusterNodes().split("\n")) {
          String[] fields = line.trim().split(" ");
          if (fields.length > 2 && (fields[2].contains("slave") || fields[2].contains("replica"))) {
            replicaLines.add(line.trim());
          }
        }
        obs.check("CLUSTER NODES via " + node + " lists only masters",
          () -> assertTrue(replicaLines.isEmpty(), "replica lines: " + replicaLines));

        String replication = jedis.info("replication");
        String role = infoField(replication, "role");
        int connectedReplicas = Integer.parseInt(infoField(replication, "connected_slaves"));
        log.info("{}: INFO replication via {}: role={}, connected_slaves={}", scenario, node, role,
          connectedReplicas);
        obs.check("INFO replication via " + node + " shows a master with replicas behind it",
          () -> {
            assertEquals("master", role, "role");
            assertTrue(connectedReplicas > 0, "connected_slaves");
          });
      }
    }
    obs.check("the client's node set is exactly the masters CLUSTER SLOTS names",
      () -> assertEquals(mastersSeen, clientNodes));

    // the client built to read from replicas: no replica pools, so no node beyond the masters
    RedisClusterClient readFromReplicas = openClient(database,
      MaintenanceNotificationsConfig.DISABLED, config -> config.readOnlyForRedisClusterReplicas(),
      builder -> {
      });
    obs.check("a readOnlyForRedisClusterReplicas client has no pool beyond the masters",
      () -> assertEquals(clientNodes, nodeKeys(readFromReplicas)));
    obs.assertAllPassed();
  }

  private static String infoField(String info, String key) {
    for (String line : info.split("\r?\n")) {
      if (line.startsWith(key + ":")) {
        return line.substring(key.length() + 1).trim();
      }
    }
    throw new AssertionError("INFO field " + key + " missing in: " + info);
  }

  /** What a raw CLUSTER SLOTS reply says about masters and replicas. */
  private static final class SlotsView {
    int ranges;
    int slotsCovered;
    final Set<String> masters = new LinkedHashSet<>();
    /** {@code host:port [start-end]} of every node entry after the master. */
    final List<String> replicas = new ArrayList<>();

    @SuppressWarnings("unchecked")
    static SlotsView parse(List<Object> reply) {
      SlotsView view = new SlotsView();
      for (Object rangeObj : reply) {
        List<Object> range = (List<Object>) rangeObj;
        long start = (Long) range.get(0);
        long end = (Long) range.get(1);
        view.ranges++;
        view.slotsCovered += end - start + 1;
        for (int i = MASTER_NODE_INDEX; i < range.size(); i++) {
          List<Object> hostInfo = (List<Object>) range.get(i);
          if (hostInfo.isEmpty()) {
            continue;
          }
          String key = SafeEncoder.encode((byte[]) hostInfo.get(0)) + ":" + hostInfo.get(1);
          if (i == MASTER_NODE_INDEX) {
            view.masters.add(key);
          } else {
            view.replicas.add(key + " [" + start + "-" + end + "]");
          }
        }
      }
      return view;
    }
  }
}
