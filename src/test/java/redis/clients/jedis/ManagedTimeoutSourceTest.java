package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Unit tests for {@link ManagedTimeoutSource}: a chain link whose opinion is whatever its supplier
 * says right now, and whose identity for {@link ChainedTimeoutSource#seekBy} is the owner that
 * installed it.
 */
public class ManagedTimeoutSourceTest {

  private static final TimeoutInfo BASE = new TimeoutInfo(2000, 5000);
  private static final TimeoutInfo RELAXED = new TimeoutInfo(10_000, 15_000);
  private static final TimeoutInfo OTHER = new TimeoutInfo(20_000, 25_000);

  private final Object owner = new Object();

  @Test
  public void supplierOpinionOverridesTheBase() {
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> RELAXED, owner));

    assertSame(RELAXED, base.get());
  }

  @Test
  public void silentSupplierFallsThroughToTheBase() {
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> null, owner));

    assertSame(BASE, base.get());
  }

  @Test
  public void supplierIsConsultedOnEveryRead() {
    AtomicReference<TimeoutInfo> gate = new AtomicReference<>();
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(gate::get, owner));

    assertSame(BASE, base.get());
    gate.set(RELAXED);
    assertSame(RELAXED, base.get());
    gate.set(null);
    assertSame(BASE, base.get());
  }

  @Test
  public void identityIsTheOwnerNotTheSupplierOrTheClass() {
    Supplier<TimeoutInfo> supplier = () -> RELAXED;
    ManagedTimeoutSource source = new ManagedTimeoutSource(supplier, owner);
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(source);

    assertSame(source, base.seekBy(owner));
    assertNull(base.seekBy(supplier), "the supplier is the opinion, not the handle");
    assertNull(base.seekBy(ManagedTimeoutSource.class),
      "several managed sources may share a chain, so the class is not a usable identity");
    assertNull(base.seekBy(new Object()), "a different owner is a different source");
  }

  @Test
  public void sourcesOfDistinctOwnersAreTornDownIndependently() {
    Object first = new Object();
    Object second = new Object();
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> RELAXED, first));
    base.addOverride(new ManagedTimeoutSource(() -> OTHER, second));
    assertSame(OTHER, base.get(), "the last added override takes precedence");

    base.removeOverride(base.seekBy(second));
    assertSame(RELAXED, base.get());
    assertNull(base.seekBy(second));

    base.removeOverride(base.seekBy(first));
    assertSame(BASE, base.get());
    assertNull(base.seekBy(first));
  }

  @Test
  public void oneOwnerMaySupplyTheSameOpinionToSeveralChains() {
    Supplier<TimeoutInfo> shared = () -> RELAXED;
    ChainedTimeoutSource one = new ChainedTimeoutSource(BASE);
    ChainedTimeoutSource two = new ChainedTimeoutSource(BASE);
    one.addOverride(new ManagedTimeoutSource(shared, owner));
    two.addOverride(new ManagedTimeoutSource(shared, owner));

    one.removeOverride(one.seekBy(owner));
    assertSame(BASE, one.get());
    assertSame(RELAXED, two.get(), "each chain holds its own link for the owner");
  }

  @Test
  public void aSilentLaterOverrideYieldsToAnEarlierOpinion() {
    AtomicReference<TimeoutInfo> later = new AtomicReference<>();
    ChainedTimeoutSource base = new ChainedTimeoutSource(BASE);
    base.addOverride(new ManagedTimeoutSource(() -> RELAXED, new Object()));
    base.addOverride(new ManagedTimeoutSource(later::get, new Object()));

    assertSame(RELAXED, base.get());
    later.set(OTHER);
    assertSame(OTHER, base.get());
  }
}
