package redis.clients.jedis.util.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

/**
 * {@link CommandHandler} turning one {@link TcpMockServer} into a Redis Cluster node: answers
 * {@code CLUSTER SLOTS} from the shared {@link MockClusterTopology}, redirects keyed commands for
 * slots it does not own with {@code -MOVED}, keeps a tiny key/value store for {@code SET}/{@code
 * GET}, serves {@code BLPOP} (optionally delayed) and the sharded pub/sub replies, and records
 * every command it saw. Knobs let a test reject the maintenance handshake, slow down {@code HELLO},
 * or hold {@code CLUSTER SLOTS} replies at a gate. Everything else falls through to the server's
 * built-in replies.
 */
public final class ClusterSlotsResponder implements CommandHandler {

  private static final String NOPROTO_MAINT_REPLY = "ERR unknown subcommand 'MAINT_NOTIFICATIONS'";

  private final MockClusterTopology topology;
  private final HostAndPort self;
  private final List<List<String>> commands = new CopyOnWriteArrayList<>();
  private final AtomicInteger clusterSlotsCalls = new AtomicInteger();
  private final Map<String, String> store = new ConcurrentHashMap<>();

  private volatile boolean rejectMaintNotifications;
  private volatile long helloDelayMillis;
  private volatile long blockingReplyDelayMillis;
  private volatile CountDownLatch clusterSlotsEntered;
  private volatile CountDownLatch clusterSlotsGate;
  private volatile String subscribedChannel;

  public ClusterSlotsResponder(MockClusterTopology topology, HostAndPort self) {
    this.topology = topology;
    this.self = self;
  }

  // ---- knobs -------------------------------------------------------------------------------

  /**
   * Reply {@code -ERR} to {@code CLIENT MAINT_NOTIFICATIONS}, like a server without the feature.
   */
  public ClusterSlotsResponder rejectMaintNotifications(boolean reject) {
    this.rejectMaintNotifications = reject;
    return this;
  }

  /** Delay every {@code HELLO} reply, to exercise handshake read timeouts. */
  public ClusterSlotsResponder helloDelayMillis(long millis) {
    this.helloDelayMillis = millis;
    return this;
  }

  /** Delay every {@code BLPOP} reply, to exercise blocking read timeouts. */
  public ClusterSlotsResponder blockingReplyDelayMillis(long millis) {
    this.blockingReplyDelayMillis = millis;
    return this;
  }

  /**
   * Holds every {@code CLUSTER SLOTS} reply at {@code release}. The reply is snapshotted when the
   * command arrives (before waiting), so a held refresh answers with the topology as of its
   * arrival; {@code entered} is counted down on arrival.
   */
  public ClusterSlotsResponder gateClusterSlots(CountDownLatch entered, CountDownLatch release) {
    this.clusterSlotsEntered = entered;
    this.clusterSlotsGate = release;
    return this;
  }

  // ---- observation -------------------------------------------------------------------------

  public int clusterSlotsCalls() {
    return clusterSlotsCalls.get();
  }

  /** Every command received, as decoded tokens, in arrival order. */
  public List<List<String>> commands() {
    return commands;
  }

  /** The received commands whose name matches (case-insensitive). */
  public List<List<String>> commands(String command) {
    List<List<String>> matching = new ArrayList<>();
    for (List<String> tokens : commands) {
      if (command.equalsIgnoreCase(tokens.get(0))) {
        matching.add(tokens);
      }
    }
    return matching;
  }

  /** The {@code CLIENT MAINT_NOTIFICATIONS ...} handshakes received. */
  public List<List<String>> maintNotificationsHandshakes() {
    List<List<String>> matching = new ArrayList<>();
    for (List<String> tokens : commands("CLIENT")) {
      if (tokens.size() > 1 && "MAINT_NOTIFICATIONS".equalsIgnoreCase(tokens.get(1))) {
        matching.add(tokens);
      }
    }
    return matching;
  }

  /** Whether a command with the given name was received for the given key (first argument). */
  public boolean sawKey(String command, String key) {
    for (List<String> tokens : commands(command)) {
      if (tokens.size() > 1 && key.equals(tokens.get(1))) {
        return true;
      }
    }
    return false;
  }

  // ---- handler -----------------------------------------------------------------------------

  @Override
  public String handleCommand(CommandArguments args, String clientId) {
    List<String> tokens = new ArrayList<>();
    args.forEach(arg -> tokens.add(SafeEncoder.encode(arg.getRaw())));
    commands.add(tokens);
    String command = tokens.get(0).toUpperCase();
    switch (command) {
      case "HELLO":
        sleep(helloDelayMillis);
        return null;
      case "CLIENT":
        if (rejectMaintNotifications && tokens.size() > 1
            && "MAINT_NOTIFICATIONS".equalsIgnoreCase(tokens.get(1))) {
          return RespResponse.error(NOPROTO_MAINT_REPLY);
        }
        return null;
      case "CLUSTER":
        return tokens.size() > 1 && "SLOTS".equalsIgnoreCase(tokens.get(1)) ? clusterSlots() : null;
      case "SET": {
        String moved = movedIfForeign(tokens.get(1));
        if (moved != null) {
          return moved;
        }
        store.put(tokens.get(1), tokens.get(2));
        return RespResponse.ok();
      }
      case "GET": {
        String moved = movedIfForeign(tokens.get(1));
        return moved != null ? moved : RespResponse.bulkString(store.get(tokens.get(1)));
      }
      case "BLPOP": {
        String moved = movedIfForeign(tokens.get(1));
        if (moved != null) {
          return moved;
        }
        sleep(blockingReplyDelayMillis);
        return RespResponse.arrayOfBulkStrings(tokens.get(1), "popped");
      }
      case "SSUBSCRIBE":
        subscribedChannel = tokens.get(1);
        return RespResponse.push(Arrays.<Object> asList("ssubscribe", tokens.get(1), 1));
      case "SUNSUBSCRIBE":
        return RespResponse.push(Arrays.<Object> asList("sunsubscribe",
          tokens.size() > 1 ? tokens.get(1) : subscribedChannel, 0));
      default:
        return null;
    }
  }

  private String clusterSlots() {
    List<Object> reply = topology.clusterSlotsReply(); // as of arrival, even if held at the gate
    clusterSlotsCalls.incrementAndGet();
    CountDownLatch entered = clusterSlotsEntered;
    CountDownLatch gate = clusterSlotsGate;
    if (entered != null) {
      entered.countDown();
    }
    if (gate != null) {
      try {
        gate.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return RespResponse.array(reply);
  }

  /** {@code -MOVED slot host:port} when another node owns the key's slot, else {@code null}. */
  private String movedIfForeign(String key) {
    int slot = JedisClusterCRC16.getSlot(key);
    HostAndPort owner = topology.ownerOf(slot);
    if (owner == null || owner.equals(self)) {
      return null;
    }
    return RespResponse.error("MOVED " + slot + " " + owner.getHost() + ":" + owner.getPort());
  }

  private static void sleep(long millis) {
    if (millis <= 0) {
      return;
    }
    long deadline = System.currentTimeMillis() + millis;
    long remaining;
    while ((remaining = deadline - System.currentTimeMillis()) > 0) {
      try {
        Thread.sleep(remaining);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
