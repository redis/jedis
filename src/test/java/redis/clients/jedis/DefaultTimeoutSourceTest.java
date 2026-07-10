package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Fusing behavior of {@link DefaultTimeoutSource}: an override (maintenance relaxation) is fused
 * per field with the configured value, taking the looser of the two — {@code 0} (infinite) being
 * the loosest — so relaxation can never tighten the configured deadline.
 */
public class DefaultTimeoutSourceTest {

  private static DefaultTimeoutSource source(int timeout, int blockingTimeout) {
    return new DefaultTimeoutSource(new TimeoutInfo(timeout, blockingTimeout));
  }

  private static ChainedTimeoutSource override(int timeout, int blockingTimeout) {
    return new ChainedTimeoutSource(new TimeoutInfo(timeout, blockingTimeout));
  }

  @Test
  public void noOverrideReturnsDefaults() {
    DefaultTimeoutSource source = source(2000, 5000);
    assertSame(source.getDefaults(), source.get());
  }

  @Test
  public void looserOverrideWins() {
    DefaultTimeoutSource source = source(2000, 5000);
    source.addOverride(override(10000, 15000));
    assertEquals(10000, source.get().timeout);
    assertEquals(15000, source.get().blockingTimeout);
  }

  @Test
  public void tighterOverrideLosesToDefaults() {
    DefaultTimeoutSource source = source(20000, 30000);
    source.addOverride(override(10000, 15000));
    assertEquals(20000, source.get().timeout);
    assertEquals(30000, source.get().blockingTimeout);
  }

  @Test
  public void fusingIsAppliedPerField() {
    DefaultTimeoutSource source = source(2000, 30000);
    source.addOverride(override(10000, 15000));
    assertEquals(10000, source.get().timeout, "looser relaxed timeout should win");
    assertEquals(30000, source.get().blockingTimeout, "looser configured timeout should win");
  }

  @Test
  public void infiniteConfiguredTimeoutStaysInfinite() {
    DefaultTimeoutSource source = source(0, 0);
    source.addOverride(override(10000, 15000));
    assertEquals(0, source.get().timeout);
    assertEquals(0, source.get().blockingTimeout);
  }

  @Test
  public void infiniteRelaxedTimeoutLoosensToInfinite() {
    DefaultTimeoutSource source = source(2000, 5000);
    source.addOverride(override(0, 0));
    assertEquals(0, source.get().timeout);
    assertEquals(0, source.get().blockingTimeout);
  }

  @Test
  public void fusedResultIsMemoizedAcrossReads() {
    DefaultTimeoutSource source = source(2000, 30000);
    source.addOverride(override(10000, 15000));
    assertSame(source.get(), source.get());
  }

  @Test
  public void changingDefaultsRecomputesFusedResult() {
    DefaultTimeoutSource source = source(2000, 5000);
    source.addOverride(override(10000, 15000));
    assertEquals(10000, source.get().timeout);

    source.setDefaults(25000, 5000);
    assertEquals(25000, source.get().timeout, "raised configured timeout should win the fusing");
    assertEquals(15000, source.get().blockingTimeout);
  }

  @Test
  public void invalidOverrideFallsBackToDefaults() {
    DefaultTimeoutSource source = source(2000, 5000);
    ExpiringTimeoutSource expired = new ExpiringTimeoutSource(new TimeoutInfo(10000, 15000));
    source.addOverride(expired);
    // expiration time never set: the override window is closed, so no fusing is involved.
    assertSame(source.getDefaults(), source.get());
  }
}
