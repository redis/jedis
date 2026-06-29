package redis.clients.jedis;

import redis.clients.jedis.TimeoutSupplier.DefaultTimeoutCard;
import redis.clients.jedis.TimeoutSupplier.TimeoutInfo;

public class MaintenanceAwareTimeoutCard extends DefaultTimeoutCard {

  private final MaintenanceEventController controller;
  private final int relaxedTimeout;
  private final int relaxedBlockingTimeout;

  MaintenanceAwareTimeoutCard(int timeout, int blockingTimeout,
      MaintenanceEventController controller, int relaxedTimeout, int relaxedBlockingTimeout) {
    super(timeout, blockingTimeout);
    this.controller = controller;
    this.relaxedTimeout = relaxedTimeout;
    this.relaxedBlockingTimeout = relaxedBlockingTimeout;

  }

  @Override
  public TimeoutInfo getInfo() {
    if (controller.isRebindActive()) {
      return new TimeoutInfo(relaxedTimeout, relaxedBlockingTimeout, Long.MAX_VALUE);
    }
    return super.getInfo();
  }
}