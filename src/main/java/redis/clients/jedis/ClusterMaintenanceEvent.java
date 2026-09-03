package redis.clients.jedis;

import java.util.Collections;
import java.util.List;

/**
 * A cluster maintenance event (OSS cluster mode). A hierarchy deliberately separate from
 * {@link MaintenanceEvent}: the two message families are exclusive contracts — a connection
 * receives one family or the other, never both.
 */
abstract class ClusterMaintenanceEvent extends MaintenanceEvent {

  ClusterMaintenanceEvent(long seq) {
    super(seq);
  }
}

/**
 * {@code [SMIGRATING, seq, slots-or-ranges]} — a slot migration is starting; relax timeouts until
 * the SMIGRATED with the same seq.
 */
final class SMigratingEvent extends ClusterMaintenanceEvent {
  final HashSlotRanges slots;

  SMigratingEvent(long seq, HashSlotRanges slots) {
    super(seq);
    this.slots = slots;
  }

  @Override
  void accept(MaintenanceEventListener listener, Connection conn) {
    listener.onSMigrating(this, conn);
  }

}

/**
 * {@code [SMIGRATED, seq, [[src, dest, slots-or-ranges], ...]]} — the migration ended; unrelax,
 * apply the slot delta, and retire connections to nodes left without slots.
 */
final class SMigratedEvent extends ClusterMaintenanceEvent {
  final List<SlotMigration> migrations;

  SMigratedEvent(long seq, List<SlotMigration> migrations) {
    super(seq);
    this.migrations = Collections.unmodifiableList(migrations);
  }

  @Override
  void accept(MaintenanceEventListener listener, Connection conn) {
    listener.onSMigrated(this, conn);
  }
}

/** One SMIGRATED entry: the given slots moved from {@code src} to {@code dest}. */
final class SlotMigration {
  final HostAndPort src;
  final HostAndPort dest;
  final HashSlotRanges slots;

  SlotMigration(HostAndPort src, HostAndPort dest, HashSlotRanges slots) {
    this.src = src;
    this.dest = dest;
    this.slots = slots;
  }

  @Override
  public String toString() {
    return src + " -> " + dest + " [" + slots + "]";
  }
}
