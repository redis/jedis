package redis.clients.jedis;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ClusterConnectionProviderTestSupport;

/**
 * Test bridge to package-private maintenance-notification internals of {@link Connection} and of
 * the cluster client: recording dispatched events, reading the effective (relaxed-aware) socket
 * timeout, and inspecting the cluster slot map and migration windows.
 */
public final class MaintNotificationsTestSupport {

  private MaintNotificationsTestSupport() {
  }

  /** The connection's live socket peer. */
  public static InetSocketAddress remotePeer(Connection connection) {
    return (InetSocketAddress) connection.getRemoteSocketAddress();
  }

  /**
   * The effective socket timeout in millis for non-blocking commands: the relaxed value while a
   * maintenance relaxation window is open, the configured value otherwise.
   */
  public static int effectiveTimeoutMillis(Connection connection) {
    return connection.currentTimeout();
  }

  /**
   * Whether the connection's maintenance retirement deadline has passed (it must not be reused).
   */
  public static boolean isRetired(Connection connection) {
    return connection.isRetired();
  }

  /** Registers a recorder for every maintenance event dispatched on {@code connection}. */
  public static ReceivedEvents record(Connection connection) {
    ReceivedEvents events = new ReceivedEvents();
    connection.addMaintenanceEventListener(events);
    return events;
  }

  // --- cluster client ---

  /** The node the client currently routes {@code slot} to. */
  public static HostAndPort slotOwner(RedisClusterClient client, int slot) {
    return provider(client).getNode(slot);
  }

  /** True while the client has a SMIGRATING window open (SMIGRATED not yet seen, TTL unexpired). */
  public static boolean hasActiveMigration(RedisClusterClient client) {
    ClusterMaintenanceCoordinator coordinator = coordinator(client);
    return coordinator != null && coordinator.hasActiveMigration();
  }

  /** Migration windows currently tracked by the client; 0 once every SMIGRATED was applied. */
  public static int openMigrationWindows(RedisClusterClient client) {
    ClusterMaintenanceCoordinator coordinator = coordinator(client);
    return coordinator == null ? 0 : coordinator.openMigrationWindows();
  }

  private static ClusterConnectionProvider provider(RedisClusterClient client) {
    return (ClusterConnectionProvider) client.provider;
  }

  private static ClusterMaintenanceCoordinator coordinator(RedisClusterClient client) {
    return ClusterConnectionProviderTestSupport.cache(provider(client)).maintenanceCoordinator();
  }

  /**
   * One SMIGRATED entry as a test sees it: the slots moved from {@code src} to {@code dest}. A
   * value type: the same push reaches every connection of the client, so the entries recorded on
   * two connections are equal and a collection of them can be de-duplicated with {@link #equals}.
   */
  public static final class SlotMove {
    private final SlotMigration migration;

    SlotMove(SlotMigration migration) {
      this.migration = migration;
    }

    public HostAndPort src() {
      return migration.src;
    }

    public HostAndPort dest() {
      return migration.dest;
    }

    public boolean contains(int slot) {
      return migration.slots.contains(slot);
    }

    public void forEachSlot(IntConsumer action) {
      migration.slots.forEachSlot(action);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SlotMove)) {
        return false;
      }
      SlotMove other = (SlotMove) o;
      // slots compared in wire form: equal pushes carry the identical string
      return migration.src.equals(other.migration.src)
          && migration.dest.equals(other.migration.dest)
          && migration.slots.toString().equals(other.migration.slots.toString());
    }

    @Override
    public int hashCode() {
      return Objects.hash(migration.src, migration.dest, migration.slots.toString());
    }

    @Override
    public String toString() {
      return migration.toString();
    }
  }

  /** One received maintenance event with its parsed payload, as dispatched to the connection. */
  public static final class ReceivedEvent {

    /** One of the maintenance {@link PushMessageTypes} constants. */
    public final String type;
    public final long seq;
    /** MOVING grace period or opening lead time, in seconds; -1 for closings, which carry none. */
    public final long timeSeconds;
    /** Affected shard ids, or slot ranges for the cluster family; null for MOVING. */
    public final String shardIds;
    /** MOVING target ("host:port"); null for a 'none' rebind and for non-MOVING events. */
    public final String target;
    /** SMIGRATED entries; empty for every other type. */
    public final List<SlotMove> moves;
    public final long atNanos = System.nanoTime();

    ReceivedEvent(String type, long seq, long timeSeconds, String shardIds, String target) {
      this(type, seq, timeSeconds, shardIds, target, Collections.emptyList());
    }

    ReceivedEvent(String type, long seq, long timeSeconds, String shardIds, String target,
        List<SlotMove> moves) {
      this.type = type;
      this.seq = seq;
      this.timeSeconds = timeSeconds;
      this.shardIds = shardIds;
      this.target = target;
      this.moves = Collections.unmodifiableList(moves);
    }

    @Override
    public String toString() {
      return type + "(seq=" + seq + (timeSeconds >= 0 ? ", time=" + timeSeconds : "")
          + (shardIds != null ? ", shards=" + shardIds : "")
          + (target != null ? ", target=" + target : "") + ")";
    }
  }

  /** All maintenance events received by one connection, in dispatch order, with payloads. */
  public static final class ReceivedEvents implements MaintenanceEventListener {

    private final List<ReceivedEvent> events = new CopyOnWriteArrayList<>();

    public List<ReceivedEvent> all() {
      return new CopyOnWriteArrayList<>(events);
    }

    public boolean movingReceived() {
      return received(PushMessageTypes.MOVING);
    }

    /** MIGRATING or FAILING_OVER — maintenance started, timeouts relaxed. */
    public boolean openingReceived() {
      return received(PushMessageTypes.MIGRATING) || received(PushMessageTypes.FAILING_OVER);
    }

    /** MIGRATED or FAILED_OVER — maintenance finished, timeouts restored. */
    public boolean closingReceived() {
      return received(PushMessageTypes.MIGRATED) || received(PushMessageTypes.FAILED_OVER);
    }

    public boolean sMigratingReceived() {
      return received(PushMessageTypes.SMIGRATING);
    }

    public boolean sMigratedReceived() {
      return received(PushMessageTypes.SMIGRATED);
    }

    public boolean received(String type) {
      for (ReceivedEvent event : events) {
        if (type.equals(event.type)) {
          return true;
        }
      }
      return false;
    }

    /** Nanos of the most recent event, or -1 when none arrived yet. */
    public long lastEventAtNanos() {
      return events.isEmpty() ? -1 : events.get(events.size() - 1).atNanos;
    }

    @Override
    public void onMoving(MovingEvent e, Connection c) {
      events.add(new ReceivedEvent(PushMessageTypes.MOVING, e.seq, e.gracePeriodSeconds, null,
          e.target == null ? null : e.target.toString()));
    }

    @Override
    public void onMigrating(MigratingEvent e, Connection c) {
      events.add(
        new ReceivedEvent(PushMessageTypes.MIGRATING, e.seq, e.startsInSeconds, e.shardIds, null));
    }

    @Override
    public void onMigrated(MigratedEvent e, Connection c) {
      events.add(new ReceivedEvent(PushMessageTypes.MIGRATED, e.seq, -1, e.shardIds, null));
    }

    @Override
    public void onFailingOver(FailingOverEvent e, Connection c) {
      events.add(new ReceivedEvent(PushMessageTypes.FAILING_OVER, e.seq, e.startsInSeconds,
          e.shardIds, null));
    }

    @Override
    public void onFailedOver(FailedOverEvent e, Connection c) {
      events.add(new ReceivedEvent(PushMessageTypes.FAILED_OVER, e.seq, -1, e.shardIds, null));
    }

    @Override
    public void onSMigrating(SMigratingEvent e, Connection c) {
      events.add(
        new ReceivedEvent(PushMessageTypes.SMIGRATING, e.seq, -1, String.valueOf(e.slots), null));
    }

    @Override
    public void onSMigrated(SMigratedEvent e, Connection c) {
      List<SlotMove> moves = new ArrayList<>(e.migrations.size());
      for (SlotMigration migration : e.migrations) {
        moves.add(new SlotMove(migration));
      }
      events.add(new ReceivedEvent(PushMessageTypes.SMIGRATED, e.seq, -1,
          String.valueOf(e.migrations), null, moves));
    }
  }

}
