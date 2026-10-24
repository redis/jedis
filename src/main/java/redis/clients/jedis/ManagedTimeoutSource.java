package redis.clients.jedis;

import java.util.function.Supplier;

/**
 * Relax overlay whose opinion is the supplier's: relaxed timeouts while the supplier returns them,
 * none when it returns {@code null}. Identity for {@link #seekBy} is the installing owner, never
 * the class, so several managed sources can share a chain and the owner that installed one unlinks
 * exactly its own link.
 */
final class ManagedTimeoutSource extends ChainedTimeoutSource {

  private final Supplier<TimeoutInfo> supplier;

  /** @param identity the owner to seek this link by, typically the controller installing it */
  ManagedTimeoutSource(Supplier<TimeoutInfo> supplier, Object identity) {
    super(null, identity);
    this.supplier = supplier;
  }

  @Override
  protected TimeoutInfo getOwnInfo() {
    return supplier.get();
  }
}