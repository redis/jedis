package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.MaintenancePushCodec.build;
import static redis.clients.jedis.MaintenancePushCodec.PushType.resolve;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.MaintenancePushCodec.PushType;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit coverage for {@link MaintenancePushCodec}: {@link PushType#resolve} token classification and
 * {@link MaintenancePushCodec#build} field extraction / malformed-frame rejection, for both the
 * standalone ({@link MaintenanceEvent}) and cluster ({@link ClusterMaintenanceEvent}) families.
 */
@Tag("unit")
public class MaintenancePushCodecTest {

  private static final byte[] SHARDS = bytes("[\"2\",\"4\"]");

  // resolve(): token -> PushType

  @Test
  public void resolveClassifiesMaintenanceTokens() {
    assertSame(PushType.MOVING, resolve(type("MOVING")));
    assertSame(PushType.MIGRATING, resolve(type("MIGRATING")));
    assertSame(PushType.MIGRATED, resolve(type("MIGRATED")));
    assertSame(PushType.FAILING_OVER, resolve(type("FAILING_OVER")));
    assertSame(PushType.FAILED_OVER, resolve(type("FAILED_OVER")));
  }

  @Test
  public void resolveReturnsNullForNonMaintenanceTokens() {
    assertNull(resolve(null));
    assertNull(resolve(type("message")));
    assertNull(resolve(type("invalidate")));
    assertNull(resolve(type("foobar"))); // length 6, not MOVING
    assertNull(resolve(new PushMessage(Collections.emptyList()).getType())); // empty frame
  }

  // build(): frame -> event field extraction

  @Test
  public void buildMoving() {
    MovingEvent m = assertInstanceOf(MovingEvent.class,
      build(PushType.MOVING, push(type("MOVING"), 30L, 15L, bytes("new-host:6380"))));
    assertEquals(30L, m.seq);
    assertEquals(15L, m.gracePeriodSeconds);
    assertEquals(new HostAndPort("new-host", 6380), m.target);
  }

  @Test
  public void buildMovingNoneTarget() { // null endpoint => 'none' type, no remap
    MovingEvent m = assertInstanceOf(MovingEvent.class,
      build(PushType.MOVING, push(type("MOVING"), 30L, 15L, null)));
    assertEquals(30L, m.seq);
    assertEquals(15L, m.gracePeriodSeconds);
    assertNull(m.target);
  }

  @Test
  public void buildMigrating() {
    MigratingEvent e = assertInstanceOf(MigratingEvent.class,
      build(PushType.MIGRATING, push(type("MIGRATING"), 6L, 2L, SHARDS)));
    assertEquals(6L, e.seq);
    assertEquals(2L, e.startsInSeconds);
    assertEquals("[\"2\",\"4\"]", e.shardIds);
  }

  @Test
  public void buildFailingOver() {
    FailingOverEvent e = assertInstanceOf(FailingOverEvent.class,
      build(PushType.FAILING_OVER, push(type("FAILING_OVER"), 6L, 2L, SHARDS)));
    assertEquals(6L, e.seq);
    assertEquals(2L, e.startsInSeconds);
    assertEquals("[\"2\",\"4\"]", e.shardIds);
  }

  @Test
  public void buildMigrated() {
    MigratedEvent e = assertInstanceOf(MigratedEvent.class,
      build(PushType.MIGRATED, push(type("MIGRATED"), 7L, SHARDS)));
    assertEquals(7L, e.seq);
    assertEquals("[\"2\",\"4\"]", e.shardIds);
  }

  @Test
  public void buildFailedOver() {
    FailedOverEvent e = assertInstanceOf(FailedOverEvent.class,
      build(PushType.FAILED_OVER, push(type("FAILED_OVER"), 7L, SHARDS)));
    assertEquals(7L, e.seq);
    assertEquals("[\"2\",\"4\"]", e.shardIds);
  }

  // build(): malformed frame -> throws MalformedMaintenanceEventException (logged by the consumer)

  @Test
  public void buildRejectsMalformedMoving() {
    // bad seq
    assertMalformed(PushType.MOVING, push(type("MOVING"), bytes("x"), 15L, bytes("h:1")));
    // bad time
    assertMalformed(PushType.MOVING, push(type("MOVING"), 30L, bytes("x"), bytes("h:1")));
    // missing time/target
    assertMalformed(PushType.MOVING, push(type("MOVING"), 30L));
    // missing target
    assertMalformed(PushType.MOVING, push(type("MOVING"), 30L, 15L));
    // target not byte[]
    assertMalformed(PushType.MOVING, push(type("MOVING"), 30L, 15L, 6379L));
    // unparseable host:port
    assertMalformed(PushType.MOVING, push(type("MOVING"), 30L, 15L, bytes("no-port")));
  }

  @Test
  public void buildRejectsMalformedMigrating() {
    assertMalformed(PushType.MIGRATING, push(type("MIGRATING"))); // no seq/time
    assertMalformed(PushType.MIGRATING, push(type("MIGRATING"), 6L)); // missing time
    assertMalformed(PushType.MIGRATING, push(type("MIGRATING"), 6L, bytes("x"))); // bad time
    assertMalformed(PushType.MIGRATING, push(type("MIGRATING"), bytes("x"), 2L)); // bad seq
    assertMalformed(PushType.MIGRATING, push(type("MIGRATING"), 6L, 2L)); // missing shards
  }

  @Test
  public void buildRejectsMalformedFailingOver() {
    assertMalformed(PushType.FAILING_OVER, push(type("FAILING_OVER"), bytes("x"), 2L)); // bad seq
    assertMalformed(PushType.FAILING_OVER, push(type("FAILING_OVER"), 6L)); // missing time
    assertMalformed(PushType.FAILING_OVER, push(type("FAILING_OVER"), 6L, 2L)); // missing shards
  }

  @Test
  public void buildRejectsMalformedMigrated() {
    assertMalformed(PushType.MIGRATED, push(type("MIGRATED"))); // no seq
    assertMalformed(PushType.MIGRATED, push(type("MIGRATED"), bytes("x"))); // bad seq
    assertMalformed(PushType.MIGRATED, push(type("MIGRATED"), 7L)); // missing shards
  }

  @Test
  public void buildRejectsMalformedFailedOver() {
    assertMalformed(PushType.FAILED_OVER, push(type("FAILED_OVER"))); // no seq
    assertMalformed(PushType.FAILED_OVER, push(type("FAILED_OVER"), bytes("x"))); // bad seq
    assertMalformed(PushType.FAILED_OVER, push(type("FAILED_OVER"), 7L)); // missing shards
  }

  // resolve()/build(): cluster maintenance pushes (SMIGRATING / SMIGRATED)

  @Test
  public void resolveClassifiesClusterMaintenanceTokens() {
    assertSame(PushType.SMIGRATING, resolve(type("SMIGRATING")));
    assertSame(PushType.SMIGRATED, resolve(type("SMIGRATED")));
  }

  @Test
  public void resolveDisambiguatesSameLengthTokens() {
    // MIGRATING and SMIGRATED share length 9: neither may shadow the other or admit lookalikes
    assertSame(PushType.MIGRATING, resolve(type("MIGRATING")));
    assertSame(PushType.SMIGRATED, resolve(type("SMIGRATED")));
    assertNull(resolve(type("SMIGRATEX")));
    assertNull(resolve(type("XMIGRATED")));
    assertNull(resolve(type("SMIGRATINX"))); // length 10, not SMIGRATING
    assertNull(resolve(type("smigrating"))); // tokens are case-sensitive
    assertNull(resolve(type("smigrated")));
  }

  @Test
  public void buildSMigrating() {
    SMigratingEvent e = assertInstanceOf(SMigratingEvent.class,
      build(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, bytes("123,456,789-1000"))));
    assertEquals(11L, e.seq);
    assertEquals("123,456,789-1000", e.slots.toString());
    assertTrue(e.slots.contains(123));
    assertTrue(e.slots.contains(456));
    assertTrue(e.slots.contains(1000));
    assertFalse(e.slots.contains(1001));
  }

  @Test
  public void buildSMigrated() {
    SMigratedEvent e = assertInstanceOf(SMigratedEvent.class,
      build(PushType.SMIGRATED,
        sMigrated(12L, entry(bytes("10.0.0.1:7000"), bytes("10.0.0.2:7001"), bytes("0-100")),
          entry(bytes("node-a:7002"), bytes("node-b:7003"), bytes("200,300-310")))));
    assertEquals(12L, e.seq);
    assertEquals(2, e.migrations.size());

    SlotMigration first = e.migrations.get(0);
    assertEquals(new HostAndPort("10.0.0.1", 7000), first.src);
    assertEquals(new HostAndPort("10.0.0.2", 7001), first.dest);
    assertEquals("0-100", first.slots.toString());

    SlotMigration second = e.migrations.get(1);
    assertEquals(new HostAndPort("node-a", 7002), second.src);
    assertEquals(new HostAndPort("node-b", 7003), second.dest);
    assertTrue(second.slots.contains(200));
    assertTrue(second.slots.contains(305));
    assertFalse(second.slots.contains(299));
  }

  @Test
  public void buildSMigratedWithNoEntries() { // an empty delta is a well-formed terminator
    SMigratedEvent e = assertInstanceOf(SMigratedEvent.class,
      build(PushType.SMIGRATED, push(type("SMIGRATED"), 12L, Collections.emptyList())));
    assertEquals(12L, e.seq);
    assertTrue(e.migrations.isEmpty());
  }

  @Test
  public void buildSMigratedTrailingEntryFieldsAreIgnored() { // forward-compatible: >3 fields OK
    SMigratedEvent e = assertInstanceOf(SMigratedEvent.class,
      build(PushType.SMIGRATED, sMigrated(1L,
        Arrays.asList(bytes("h1:7000"), bytes("h2:7001"), bytes("5"), bytes("extra")))));
    assertEquals(1, e.migrations.size());
    assertEquals("5", e.migrations.get(0).slots.toString());
  }

  @Test
  public void buildRejectsMalformedSMigrating() {
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"))); // no seq
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L)); // missing slots
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), bytes("x"), bytes("1-5"))); // bad
                                                                                              // seq
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, 5L)); // slots not byte[]
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, null)); // null slots
    // unparseable slots-or-ranges
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, bytes("")));
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, bytes("abc")));
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, bytes("10-9")));
    assertMalformed(PushType.SMIGRATING, push(type("SMIGRATING"), 11L, bytes("16384")));
  }

  @Test
  public void buildRejectsMalformedSMigrated() {
    List<Object> ok = entry(bytes("h1:7000"), bytes("h2:7001"), bytes("0-100"));

    assertMalformed(PushType.SMIGRATED, push(type("SMIGRATED"))); // no seq
    assertMalformed(PushType.SMIGRATED, push(type("SMIGRATED"), 12L)); // missing entries
    assertMalformed(PushType.SMIGRATED,
      push(type("SMIGRATED"), bytes("x"), Collections.singletonList(ok))); // bad seq
    assertMalformed(PushType.SMIGRATED, push(type("SMIGRATED"), 12L, bytes("not-a-list")));
    assertMalformed(PushType.SMIGRATED, push(type("SMIGRATED"), 12L, null));

    // entry not a list
    assertMalformed(PushType.SMIGRATED, push(type("SMIGRATED"), 12L, Arrays.asList(bytes("x"))));
    // entry too short
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, Arrays.asList(bytes("h1:7000"), bytes("h2:7001"))));
    // entry fields of the wrong type
    assertMalformed(PushType.SMIGRATED, sMigrated(12L, entry(7000L, bytes("h2:7001"), bytes("0"))));
    assertMalformed(PushType.SMIGRATED, sMigrated(12L, entry(bytes("h1:7000"), 7001L, bytes("0"))));
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, entry(bytes("h1:7000"), bytes("h2:7001"), 5L)));
    assertMalformed(PushType.SMIGRATED, sMigrated(12L, entry(null, bytes("h2:7001"), bytes("0"))));
    // unparseable node addresses
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, entry(bytes("no-port"), bytes("h2:7001"), bytes("0-100"))));
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, entry(bytes("h1:7000"), bytes("h2:notaport"), bytes("0-100"))));
    // unparseable slots
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, entry(bytes("h1:7000"), bytes("h2:7001"), bytes("100-0"))));
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, entry(bytes("h1:7000"), bytes("h2:7001"), bytes(""))));
    // one bad entry rejects the whole frame
    assertMalformed(PushType.SMIGRATED,
      sMigrated(12L, ok, entry(bytes("h1:7000"), bytes("h2:7001"), bytes("bad"))));
  }

  private static void assertMalformed(PushType type, PushMessage msg) {
    assertThrows(MalformedMaintenanceEventException.class, () -> build(type, msg));
  }

  private static PushMessage push(Object... content) {
    return new PushMessage(Arrays.asList(content));
  }

  /** {@code [SMIGRATED, seq, [entries...]]} */
  private static PushMessage sMigrated(long seq, List<?>... entries) {
    return push(type("SMIGRATED"), seq, Arrays.asList(entries));
  }

  /** One SMIGRATED entry: {@code [src, dest, slots]}. */
  private static List<Object> entry(Object src, Object dest, Object slots) {
    return Arrays.asList(src, dest, slots);
  }

  private static byte[] type(String t) {
    return SafeEncoder.encode(t);
  }

  private static byte[] bytes(String s) {
    return SafeEncoder.encode(s);
  }
}
