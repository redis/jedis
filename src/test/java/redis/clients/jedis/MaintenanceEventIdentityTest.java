package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Identity semantics of {@link MaintenanceEvent#identity()}: seq for all types (including the
 * cluster SMIGRATING/SMIGRATED family); MOVING is also keyed by the original target endpoint, so
 * concurrent MOVINGs to different endpoints stay distinct operations. Identity excludes payload
 * that may vary between deliveries of the same operation (remaining time, shard diagnostics, slot
 * ranges).
 */
@Tag("sch")
public class MaintenanceEventIdentityTest {

  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);
  private static final HostAndPort TARGET_C = new HostAndPort("node-c.example.com", 6381);

  @Test
  public void seqIsTheIdentityForNonMovingEvents() {
    assertEquals(new FailingOverEvent(5L, 10, "1").identity(),
      new FailingOverEvent(5L, 99, "2").identity(), "same seq: same operation");
    assertEquals(new FailingOverEvent(5L, 10, "1").identity().hashCode(),
      new FailingOverEvent(5L, 99, "2").identity().hashCode());

    assertNotEquals(new FailingOverEvent(5L, 10, "1").identity(),
      new FailingOverEvent(6L, 10, "1").identity());
  }

  @Test
  public void movingIdentity_includesTargetExcludesTtl() {
    assertEquals(new MovingEvent(5L, 10, TARGET_B).identity(),
      new MovingEvent(5L, 99, TARGET_B).identity(),
      "re-delivery with adjusted remaining time: same operation");
    assertEquals(new MovingEvent(5L, 10, TARGET_B).identity().hashCode(),
      new MovingEvent(5L, 99, TARGET_B).identity().hashCode());

    assertNotEquals(new MovingEvent(5L, 10, TARGET_B).identity(),
      new MovingEvent(5L, 10, TARGET_C).identity(),
      "seq is per-source: same seq to another target is a distinct operation");
    assertNotEquals(new MovingEvent(5L, 10, TARGET_B).identity(),
      new MovingEvent(5L, 10, null).identity(), "'none' is a distinct identity");
    assertEquals(new MovingEvent(5L, 10, null).identity(), new MovingEvent(5L, 20, null).identity(),
      "'none' identities merge by seq");
  }

  @Test
  public void movingIdentity_neverEqualsSeqOnlyIdentity() {
    // A MOVING identity is a composite value; it can never equal a plain seq, in either direction.
    assertNotEquals(new MovingEvent(5L, 10, null).identity(),
      new FailingOverEvent(5L, 10, "1").identity());
    assertNotEquals(new FailingOverEvent(5L, 10, "1").identity(),
      new MovingEvent(5L, 10, null).identity());
  }

  @Test
  public void clusterEventsUseSeqIdentity() {
    HashSlotRanges slotsA = HashSlotRanges.parse("0-100");
    HashSlotRanges slotsB = HashSlotRanges.parse("200");
    List<SlotMigration> delta = Collections
        .singletonList(new SlotMigration(TARGET_B, TARGET_C, slotsA));

    assertEquals(new SMigratingEvent(5L, slotsA).identity(),
      new SMigratingEvent(5L, slotsB).identity(), "same seq: same operation regardless of slots");
    assertEquals(new SMigratingEvent(5L, slotsA).identity().hashCode(),
      new SMigratingEvent(5L, slotsB).identity().hashCode());
    assertNotEquals(new SMigratingEvent(5L, slotsA).identity(),
      new SMigratingEvent(6L, slotsA).identity());

    assertEquals(new SMigratedEvent(5L, delta).identity(),
      new SMigratedEvent(5L, Collections.emptyList()).identity(),
      "same seq: same operation regardless of the slot delta");
    assertNotEquals(new SMigratedEvent(5L, delta).identity(),
      new SMigratedEvent(6L, delta).identity());

    assertEquals(new SMigratingEvent(5L, slotsA).identity(),
      new SMigratedEvent(5L, delta).identity(),
      "an SMIGRATED terminates the SMIGRATING with the same seq");
  }
}
