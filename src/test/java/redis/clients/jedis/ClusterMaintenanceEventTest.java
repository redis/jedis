package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the {@link ClusterMaintenanceEvent} hierarchy: each event dispatches to its own
 * listener callback with the owning connection, SMIGRATED exposes its delta read-only, and
 * {@link SlotMigration} renders as {@code src -> dest [slots]}.
 */
@Tag("unit")
public class ClusterMaintenanceEventTest {

  private static final HostAndPort SRC = new HostAndPort("10.0.0.1", 7000);
  private static final HostAndPort DEST = new HostAndPort("10.0.0.2", 7001);

  private final MaintenanceEventListener listener = mock(MaintenanceEventListener.class);
  private final Connection connection = mock(Connection.class);

  @Test
  public void sMigratingDispatchesToOnSMigrating() {
    SMigratingEvent e = new SMigratingEvent(3L, HashSlotRanges.parse("0-100"));

    e.accept(listener, connection);

    verify(listener).onSMigrating(e, connection);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void sMigratedDispatchesToOnSMigrated() {
    SMigratedEvent e = new SMigratedEvent(4L,
        Collections.singletonList(new SlotMigration(SRC, DEST, HashSlotRanges.parse("0-100"))));

    e.accept(listener, connection);

    verify(listener).onSMigrated(e, connection);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void clusterEventsAreMaintenanceEvents() { // the codec/consumer handle one
                                                    // MaintenanceEvent type
    assertInstanceOf(MaintenanceEvent.class, new SMigratingEvent(1L, HashSlotRanges.parse("1")));
    assertInstanceOf(MaintenanceEvent.class, new SMigratedEvent(1L, Collections.emptyList()));
    assertEquals(1L, new SMigratingEvent(1L, HashSlotRanges.parse("1")).seq);
    assertEquals(2L, new SMigratedEvent(2L, Collections.emptyList()).seq);
  }

  @Test
  public void sMigratingExposesSlots() {
    HashSlotRanges slots = HashSlotRanges.parse("123,456,789-1000");
    assertSame(slots, new SMigratingEvent(1L, slots).slots);
  }

  @Test
  public void sMigratedMigrationsAreReadOnly() {
    List<SlotMigration> source = new ArrayList<>(
        Arrays.asList(new SlotMigration(SRC, DEST, HashSlotRanges.parse("0-100")),
          new SlotMigration(DEST, SRC, HashSlotRanges.parse("200"))));
    SMigratedEvent e = new SMigratedEvent(1L, source);

    assertEquals(source, e.migrations);
    assertThrows(UnsupportedOperationException.class, () -> e.migrations.clear());
    assertThrows(UnsupportedOperationException.class,
      () -> e.migrations.add(new SlotMigration(SRC, DEST, HashSlotRanges.parse("5"))));
  }

  @Test
  public void slotMigrationToString() {
    SlotMigration m = new SlotMigration(SRC, DEST, HashSlotRanges.parse("123,456,789-1000"));
    assertEquals("10.0.0.1:7000 -> 10.0.0.2:7001 [123,456,789-1000]", m.toString());
    assertSame(SRC, m.src);
    assertSame(DEST, m.dest);
  }
}
