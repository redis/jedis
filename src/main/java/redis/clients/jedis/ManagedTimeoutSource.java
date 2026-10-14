package redis.clients.jedis;

import java.util.function.Supplier;

/**
 * Relax overlay whose opinion is the supplier's: relaxed timeouts while the supplier returns them,
 * none when it returns {@code null}. Identity for {@link #seekBy} is the supplier instance, not the
 * class, so the controller that installed it unlinks exactly its own link.
 */
final class ManagedTimeoutSource extends ChainedTimeoutSource {

  private final Supplier<TimeoutInfo> supplier;

  ManagedTimeoutSource(Supplier<TimeoutInfo> supplier) {
    super(null, supplier);
    this.supplier = supplier;
  }

  @Override
  protected TimeoutInfo getOwnInfo() {
    return supplier.get();
  }
}