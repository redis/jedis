package redis.clients.jedis;

import java.net.InetSocketAddress;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test bridge to package-private maintenance-notification internals of {@link Connection}:
 * recording dispatched events and reading the effective (relaxed-aware) socket timeout.
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

  /** Registers a recorder for every maintenance event dispatched on {@code connection}. */
  public static ReceivedEvents record(Connection connection) {
    ReceivedEvents events = new ReceivedEvents();
    connection.addMaintenanceEventListener(events);
    return events;
  }

  /** One received maintenance event with its parsed payload, as dispatched to the connection. */
  public static final class ReceivedEvent {

    /** One of the maintenance {@link PushMessageTypes} constants. */
    public final String type;
    public final long seq;
    /** MOVING grace period or opening lead time, in seconds; -1 for closings, which carry none. */
    public final long timeSeconds;
    /** Affected shard ids; null for MOVING. */
    public final String shardIds;
    /** MOVING target ("host:port"); null for a 'none' rebind and for non-MOVING events. */
    public final String target;
    public final long atNanos = System.nanoTime();

    ReceivedEvent(String type, long seq, long timeSeconds, String shardIds, String target) {
      this.type = type;
      this.seq = seq;
      this.timeSeconds = timeSeconds;
      this.shardIds = shardIds;
      this.target = target;
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

    private boolean received(String type) {
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
  }

}
