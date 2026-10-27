package redis.clients.jedis.util.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;

/**
 * The slot table a set of mock cluster nodes agree on: who owns each of the 16384 slots. Shared by
 * every {@link ClusterSlotsResponder} of one mock cluster so that {@code CLUSTER SLOTS} replies and
 * {@code MOVED} redirects stay consistent. Tests mutate it to simulate a migration that completed
 * on the server side, then announce it with an SMIGRATED push.
 */
public final class MockClusterTopology {

  private final HostAndPort[] owners = new HostAndPort[Protocol.CLUSTER_HASHSLOTS];

  /** Assigns the inclusive slot range to {@code master}, replacing any previous owner. */
  public synchronized MockClusterTopology assign(int from, int to, HostAndPort master) {
    for (int slot = from; slot <= to; slot++) {
      owners[slot] = master;
    }
    return this;
  }

  public synchronized HostAndPort ownerOf(int slot) {
    return owners[slot];
  }

  /**
   * The {@code CLUSTER SLOTS} reply for the current table: one {@code [from, to, [host, port, id]]}
   * entry per maximal run of slots with the same owner, unassigned runs omitted.
   */
  public synchronized List<Object> clusterSlotsReply() {
    List<Object> ranges = new ArrayList<>();
    int start = 0;
    for (int slot = 1; slot <= owners.length; slot++) {
      if (slot == owners.length || !Objects.equals(owners[slot], owners[start])) {
        if (owners[start] != null) {
          ranges.add(range(start, slot - 1, owners[start]));
        }
        start = slot;
      }
    }
    return ranges;
  }

  private static List<Object> range(int from, int to, HostAndPort master) {
    List<Object> node = Arrays.<Object> asList(master.getHost(), (long) master.getPort(),
      nodeId(master));
    return Arrays.<Object> asList((long) from, (long) to, node);
  }

  /** A stable, unique node id derived from the address (Jedis only reads host and port). */
  public static String nodeId(HostAndPort node) {
    return "mock-node-" + node.getHost().replace('.', '-') + "-" + node.getPort();
  }
}
