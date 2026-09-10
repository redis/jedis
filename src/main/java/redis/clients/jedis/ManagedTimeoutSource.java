package redis.clients.jedis;

import java.util.function.Supplier;

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