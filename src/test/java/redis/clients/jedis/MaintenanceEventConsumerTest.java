package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit coverage for {@link MaintenanceEventConsumer}: the dispatch/drop glue. A recognized frame is
 * decoded and fanned out to every listener (with the owning connection) then dropped — even when
 * malformed; any other push passes through untouched.
 */
@Tag("unit")
public class MaintenanceEventConsumerTest {

  private final Connection connection = mock(Connection.class);

  @Test
  public void dispatchesValidEventToAllListenersThenDrops() {
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    PushConsumerContext ctx = consume(new HashSet<>(Arrays.asList(a, b)),
      push(type("MOVING"), 30L, 15L, bytes("new-host:6380")));

    assertTrue(ctx.shouldDrop());
    for (RecordingListener l : Arrays.asList(a, b)) {
      assertEquals(1, l.calls.size());
      Call c = l.calls.get(0);
      assertEquals("onMoving", c.callback);
      assertSame(connection, c.connection);
      assertEquals(30L, assertInstanceOf(MovingEvent.class, c.event).seq);
    }
  }

  @Test
  public void malformedRecognizedFrameDropsWithoutDispatch() {
    RecordingListener l = new RecordingListener();
    // resolves as MOVING but the seq is not a Long -> build() throws, consumer discards
    PushConsumerContext ctx = consume(Collections.singletonList(l),
      push(type("MOVING"), bytes("x"), 15L, bytes("h:1")));

    assertTrue(ctx.shouldDrop());
    assertTrue(l.calls.isEmpty());
  }

  @Test
  public void nonMaintenanceFramePassesThrough() {
    RecordingListener l = new RecordingListener();
    PushConsumerContext ctx = consume(Collections.singletonList(l),
      push(type("message"), bytes("chan"), bytes("payload")));

    assertFalse(ctx.shouldDrop());
    assertFalse(ctx.shouldPropagate());
    assertTrue(l.calls.isEmpty());
  }

  @Test
  public void routesEachTypeToItsCallback() {
    byte[] shards = bytes("[\"2\"]");
    assertCallback("onMoving", push(type("MOVING"), 1L, 15L, bytes("h:6380")));
    assertCallback("onMigrating", push(type("MIGRATING"), 2L, 5L, shards));
    assertCallback("onFailingOver", push(type("FAILING_OVER"), 3L, 5L, shards));
    assertCallback("onMigrated", push(type("MIGRATED"), 4L, shards));
    assertCallback("onFailedOver", push(type("FAILED_OVER"), 5L, shards));
  }

  @Test
  public void listenerExceptionPropagates() {
    MaintenanceEventListener boom = new RecordingListener() {
      @Override
      public void onMoving(MovingEvent e, Connection c) {
        throw new IllegalStateException("boom");
      }
    };
    MaintenanceEventConsumer consumer = new MaintenanceEventConsumer(connection,
        Collections.singleton(boom));
    PushConsumerContext ctx = new PushConsumerContext(push(type("MOVING"), 30L, 15L, bytes("h:1")));

    assertThrows(IllegalStateException.class, () -> consumer.handle(ctx));
  }

  private void assertCallback(String expected, PushMessage msg) {
    RecordingListener l = new RecordingListener();
    consume(Collections.singleton(l), msg);
    assertEquals(1, l.calls.size());
    assertEquals(expected, l.calls.get(0).callback);
    assertSame(connection, l.calls.get(0).connection);
  }

  private PushConsumerContext consume(Set<MaintenanceEventListener> listeners, PushMessage msg) {
    PushConsumerContext ctx = new PushConsumerContext(msg);
    new MaintenanceEventConsumer(connection, listeners).handle(ctx);
    return ctx;
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

  private static final class Call {
    final String callback;
    final MaintenanceEvent event;
    final Connection connection;

    Call(String callback, MaintenanceEvent event, Connection connection) {
      this.callback = callback;
      this.event = event;
      this.connection = connection;
    }
  }

  private static class RecordingListener implements MaintenanceEventListener {
    final List<Call> calls = new ArrayList<>();

    @Override
    public void onMoving(MovingEvent e, Connection c) {
      calls.add(new Call("onMoving", e, c));
    }

    @Override
    public void onMigrating(MigratingEvent e, Connection c) {
      calls.add(new Call("onMigrating", e, c));
    }

    @Override
    public void onMigrated(MigratedEvent e, Connection c) {
      calls.add(new Call("onMigrated", e, c));
    }

    @Override
    public void onFailingOver(FailingOverEvent e, Connection c) {
      calls.add(new Call("onFailingOver", e, c));
    }

    @Override
    public void onFailedOver(FailedOverEvent e, Connection c) {
      calls.add(new Call("onFailedOver", e, c));
    }
  }
}
