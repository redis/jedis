package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RebindAware.RebindResult;

/**
 * Unit tests for the lock-free, sequence-guarded, time-bounded MOVING rebind overlay in
 * {@link DefaultJedisSocketFactory}. Uses an injected monotonic clock for deterministic expiry.
 */
@Tag("sch")
public class DefaultJedisSocketFactoryRebindTest {

  private static final HostAndPort ORIGINAL = new HostAndPort("original.example.com", 6379);
  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);
  private static final HostAndPort TARGET_C = new HostAndPort("node-c.example.com", 6381);

  private DefaultJedisSocketFactory factory;
  private AtomicLong now;

  @BeforeEach
  public void setUp() {
    factory = new DefaultJedisSocketFactory(ORIGINAL);
    now = new AtomicLong(0);
    factory.setClockNanos(now::get);
  }

  private void advanceSeconds(long seconds) {
    now.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  @Test
  public void noRebind_usesOriginal() {
    assertEquals(ORIGINAL, factory.getHostAndPort());
  }

  @Test
  public void applyNewTarget_thenExpiresBackToOriginal() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(1L, TARGET_B, 10));
    assertEquals(TARGET_B, factory.getHostAndPort(), "within grace window uses new target");

    advanceSeconds(9);
    assertEquals(TARGET_B, factory.getHostAndPort(), "still within window");

    advanceSeconds(2); // now past the 10s deadline
    assertEquals(ORIGINAL, factory.getHostAndPort(), "after expiry reverts to original");
  }

  @Test
  public void staleAndOutOfOrderEvents_areIgnored() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(5L, TARGET_B, 100));

    assertEquals(RebindResult.STALE, factory.rebind(5L, TARGET_C, 100), "same seq is a duplicate");
    assertEquals(RebindResult.STALE, factory.rebind(3L, TARGET_C, 100), "lower seq is out-of-order");
    assertEquals(TARGET_B, factory.getHostAndPort(), "target unchanged by stale events");
  }

  @Test
  public void higherSeqSameTarget_isApplied() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(5L, TARGET_B, 10));
    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(6L, TARGET_B, 10),
        "a strictly newer seq is always applied; dedup is by seq, not by target");
    assertEquals(TARGET_B, factory.getHostAndPort());
  }

  @Test
  public void higherSeqNewTarget_supersedes() {
    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(5L, TARGET_B, 100));
    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(6L, TARGET_C, 100));
    assertEquals(TARGET_C, factory.getHostAndPort());
  }

  @Test
  public void newEventAfterExpiry_appliesAgain() {
    factory.rebind(5L, TARGET_B, 10);
    advanceSeconds(11); // expire
    assertEquals(ORIGINAL, factory.getHostAndPort());

    assertEquals(RebindResult.APPLIED_NEW_TARGET, factory.rebind(6L, TARGET_C, 10));
    assertEquals(TARGET_C, factory.getHostAndPort(), "a newer event after expiry applies again");
  }
}