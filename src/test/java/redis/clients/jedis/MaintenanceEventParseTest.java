package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit coverage for {@link MaintenanceEvent#parse} — classification, field extraction, leniency.
 */
@Tag("unit")
public class MaintenanceEventParseTest {

  private static final byte[] SHARDS = bytes("[\"2\",\"4\"]");

  private static PushMessage push(Object... content) {
    return new PushMessage(Arrays.asList(content));
  }

  private static byte[] type(String t) {
    return SafeEncoder.encode(t);
  }

  private static byte[] bytes(String s) {
    return SafeEncoder.encode(s);
  }

  @Test
  public void moving() {
    MovingEvent m = assertInstanceOf(MovingEvent.class,
      MaintenanceEvent.parse(push(type("MOVING"), 30L, 15L, bytes("new-host:6380"))));
    assertEquals(30L, m.seq);
    assertEquals(15L, m.ttlSeconds);
    assertEquals(new HostAndPort("new-host", 6380), m.target);

    assertNull(MaintenanceEvent.parse(push(type("MOVING"), bytes("x"), 15L, bytes("h:1")))); // bad
                                                                                             // seq
    assertNull(MaintenanceEvent.parse(push(type("MOVING"), 30L, bytes("x"), bytes("h:1")))); // bad
                                                                                             // time
    assertNull(MaintenanceEvent.parse(push(type("MOVING"), 30L))); // missing time/target
    assertNull(MaintenanceEvent.parse(push(type("MOVING"), 30L, 15L))); // missing target
    assertNull(MaintenanceEvent.parse(push(type("MOVING"), 30L, 15L, 6379L))); // target not byte[]
    assertNull(MaintenanceEvent.parse(push(type("MOVING"), 30L, 15L, bytes("no-port")))); // unparseable
  }

  @Test
  public void migrating() {
    MigratingEvent e = assertInstanceOf(MigratingEvent.class,
      MaintenanceEvent.parse(push(type("MIGRATING"), 6L, 2L, SHARDS)));
    assertEquals(6L, e.seq);
    assertEquals(2L, e.ttlSeconds);
    assertEquals("[\"2\",\"4\"]", e.shardIds);

    assertNull(MaintenanceEvent.parse(push(type("MIGRATING")))); // no seq/time
    assertNull(MaintenanceEvent.parse(push(type("MIGRATING"), 6L))); // missing time
    assertNull(MaintenanceEvent.parse(push(type("MIGRATING"), 6L, bytes("x")))); // bad time
    assertNull(MaintenanceEvent.parse(push(type("MIGRATING"), bytes("x"), 2L))); // bad seq
    assertNull(MaintenanceEvent.parse(push(type("MIGRATING"), 6L, 2L))); // missing shards
  }

  @Test
  public void failingOver() {
    FailingOverEvent e = assertInstanceOf(FailingOverEvent.class,
      MaintenanceEvent.parse(push(type("FAILING_OVER"), 6L, 2L, SHARDS)));
    assertEquals(6L, e.seq);
    assertEquals(2L, e.ttlSeconds);
    assertEquals("[\"2\",\"4\"]", e.shardIds);

    assertNull(MaintenanceEvent.parse(push(type("FAILING_OVER"), bytes("x"), 2L))); // bad seq
    assertNull(MaintenanceEvent.parse(push(type("FAILING_OVER"), 6L))); // missing time
    assertNull(MaintenanceEvent.parse(push(type("FAILING_OVER"), 6L, 2L))); // missing shards
  }

  @Test
  public void migrated() {
    MigratedEvent e = assertInstanceOf(MigratedEvent.class,
      MaintenanceEvent.parse(push(type("MIGRATED"), 7L, SHARDS)));
    assertEquals(7L, e.seq);
    assertEquals("[\"2\",\"4\"]", e.shardIds);

    assertNull(MaintenanceEvent.parse(push(type("MIGRATED")))); // no seq
    assertNull(MaintenanceEvent.parse(push(type("MIGRATED"), bytes("x")))); // bad seq
    assertNull(MaintenanceEvent.parse(push(type("MIGRATED"), 7L))); // missing shards
  }

  @Test
  public void failedOver() {
    FailedOverEvent e = assertInstanceOf(FailedOverEvent.class,
      MaintenanceEvent.parse(push(type("FAILED_OVER"), 7L, SHARDS)));
    assertEquals(7L, e.seq);
    assertEquals("[\"2\",\"4\"]", e.shardIds);

    assertNull(MaintenanceEvent.parse(push(type("FAILED_OVER")))); // no seq
    assertNull(MaintenanceEvent.parse(push(type("FAILED_OVER"), bytes("x")))); // bad seq
    assertNull(MaintenanceEvent.parse(push(type("FAILED_OVER"), 7L))); // missing shards
  }

  @Test
  public void isMaintenanceTypeClassifies() {
    for (String t : new String[] { "MOVING", "MIGRATING", "MIGRATED", "FAILING_OVER",
        "FAILED_OVER" }) {
      assertTrue(MaintenanceEvent.isMaintenanceType(type(t)), t);
    }
    assertFalse(MaintenanceEvent.isMaintenanceType(null));
    assertFalse(MaintenanceEvent.isMaintenanceType(type("message")));
    assertFalse(MaintenanceEvent.isMaintenanceType(type("invalidate")));
    assertFalse(MaintenanceEvent.isMaintenanceType(type("foobar"))); // length 6, not MOVING
  }

  @Test
  public void nonMaintenanceOrEmptyReturnsNull() {
    assertNull(MaintenanceEvent.parse(push(type("message"), bytes("chan"), bytes("payload"))));
    assertNull(MaintenanceEvent.parse(new PushMessage(Collections.emptyList())));
  }
}
