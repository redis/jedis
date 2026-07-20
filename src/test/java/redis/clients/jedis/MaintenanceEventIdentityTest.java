package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Identity semantics of {@link MaintenanceEvent#identity()}: event type + seq for all types,
 * refined with the original target endpoint for MOVING. Identity excludes payload that may vary
 * between deliveries of the same operation (remaining time, shard diagnostics).
 */
@Tag("sch")
public class MaintenanceEventIdentityTest {

  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);
  private static final HostAndPort TARGET_C = new HostAndPort("node-c.example.com", 6381);

  @Test
  public void seqBasedIdentity_equalWithinType_distinctAcrossTypes() {
    assertEquals(new FailingOverEvent(5L, 10, "1").identity(),
      new FailingOverEvent(5L, 99, "2").identity(), "same type + seq: same operation");
    assertEquals(new FailingOverEvent(5L, 10, "1").identity().hashCode(),
      new FailingOverEvent(5L, 99, "2").identity().hashCode());

    assertNotEquals(new FailingOverEvent(5L, 10, "1").identity(),
      new MigratingEvent(5L, 10, "1").identity(), "same seq across types: distinct operations");
    assertNotEquals(new MigratedEvent(5L, "1").identity(), new FailedOverEvent(5L, "1").identity());
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
  public void movingIdentity_neverEqualsSeqBasedIdentity() {
    // Class-strict equality, symmetric in both directions.
    assertNotEquals(new MovingEvent(5L, 10, null).identity(),
      new FailingOverEvent(5L, 10, "1").identity());
    assertNotEquals(new FailingOverEvent(5L, 10, "1").identity(),
      new MovingEvent(5L, 10, null).identity());
  }
}