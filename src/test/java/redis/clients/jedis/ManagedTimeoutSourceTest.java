package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Unit tests for {@link ManagedTimeoutSource}: a chain link whose opinion is whatever its supplier
 * says right now, and whose identity for {@link ChainedTimeoutSource#seekBy} is that supplier.
 */
@Tag("unit")
public class ManagedTimeoutSourceTest {

  private static final TimeoutInfo BASE = new TimeoutInfo(2000, 5000);
  private static final TimeoutInfo RELAXED = new TimeoutInfo(10_000, 15_000);
  private static final TimeoutInfo OTHER = new TimeoutInfo(20_000, 25_000);

  @Test
  public void supplierOpinionOverridesTheBase() {
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> RELAXED));

    assertSame(RELAXED, base.get());
  }

  @Test
  public void silentSupplierFallsThroughToTheBase() {
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> null));

    assertSame(BASE, base.get());
  }

  @Test
  public void supplierIsConsultedOnEveryRead() {
    AtomicReference<TimeoutInfo> gate = new AtomicReference<>();
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(gate::get));

    assertSame(BASE, base.get());
    gate.set(RELAXED);
    assertSame(RELAXED, base.get());
    gate.set(null);
    assertSame(BASE, base.get());
  }

  @Test
  public void identityIsTheSupplierNotTheClass() {
    Supplier<TimeoutInfo> supplier = () -> RELAXED;
    ManagedTimeoutSource source = new ManagedTimeoutSource(supplier);
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(source);

    assertSame(source, base.seekBy(supplier));
    assertNull(base.seekBy(ManagedTimeoutSource.class),
      "several managed sources may share a chain, so the class is not a usable identity");
    Supplier<TimeoutInfo> lookalike = () -> RELAXED;
    assertNull(base.seekBy(lookalike), "a different supplier instance is a different source");
  }

  @Test
  public void sourcesOverDistinctSuppliersAreTornDownIndependently() {
    Supplier<TimeoutInfo> first = () -> RELAXED;
    Supplier<TimeoutInfo> second = () -> OTHER;
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(first));
    base.addOverride(new ManagedTimeoutSource(second));
    assertSame(OTHER, base.get(), "the last added override takes precedence");

    base.removeOverride(base.seekBy(second));
    assertSame(RELAXED, base.get());
    assertNull(base.seekBy(second));

    base.removeOverride(base.seekBy(first));
    assertSame(BASE, base.get());
    assertNull(base.seekBy(first));
  }

  @Test
  public void aSilentLaterOverrideYieldsToAnEarlierOpinion() {
    AtomicReference<TimeoutInfo> later = new AtomicReference<>();
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> RELAXED));
    base.addOverride(new ManagedTimeoutSource(later::get));

    assertSame(RELAXED, base.get());
    later.set(OTHER);
    assertSame(OTHER, base.get());
  }
}
