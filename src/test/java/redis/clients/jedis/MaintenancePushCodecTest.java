package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.MaintenancePushCodec.build;
import static redis.clients.jedis.MaintenancePushCodec.PushType.resolve;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.MaintenanceEvent.FailedOverEvent;
import redis.clients.jedis.MaintenanceEvent.FailingOverEvent;
import redis.clients.jedis.MaintenanceEvent.MigratedEvent;
import redis.clients.jedis.MaintenanceEvent.MigratingEvent;
import redis.clients.jedis.MaintenanceEvent.MovingEvent;
import redis.clients.jedis.MaintenancePushCodec.PushType;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit coverage for {@link MaintenancePushCodec}: {@link PushType#resolve} token classification and
 * {@link MaintenancePushCodec#build} field extraction / malformed-frame rejection.
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
    assertEquals(15L, m.ttlSeconds);
    assertEquals(new HostAndPort("new-host", 6380), m.target);
  }

  @Test
  public void buildMigrating() {
    MigratingEvent e = assertInstanceOf(MigratingEvent.class,
      build(PushType.MIGRATING, push(type("MIGRATING"), 6L, 2L, SHARDS)));
    assertEquals(6L, e.seq);
    assertEquals(2L, e.ttlSeconds);
    assertEquals("[\"2\",\"4\"]", e.shardIds);
  }

  @Test
  public void buildFailingOver() {
    FailingOverEvent e = assertInstanceOf(FailingOverEvent.class,
      build(PushType.FAILING_OVER, push(type("FAILING_OVER"), 6L, 2L, SHARDS)));
    assertEquals(6L, e.seq);
    assertEquals(2L, e.ttlSeconds);
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

  private static void assertMalformed(PushType type, PushMessage msg) {
    assertThrows(MalformedMaintenanceEventException.class, () -> build(type, msg));
  }

  private static PushMessage push(Object... content) {
    return new PushMessage(Arrays.asList(content));
  }

  private static byte[] type(String t) {
    return SafeEncoder.encode(t);
  }

  private static byte[] bytes(String s) {
    return SafeEncoder.encode(s);
  }
}
