package redis.clients.jedis;

import redis.clients.jedis.TimeoutSupplier.DefaultTimeoutCard;
import redis.clients.jedis.TimeoutSupplier.TimeoutInfo;

public class MaintenanceAwareTimeoutCard extends DefaultTimeoutCard {

  private final MaintenanceEventController controller;
  private final TimeoutInfo relaxedTimeoutInfo;

  MaintenanceAwareTimeoutCard(int timeout, int blockingTimeout,
      MaintenanceEventController controller) {
    super(timeout, blockingTimeout);
    this.controller = controller;
    this.relaxedTimeoutInfo = new TimeoutInfo(controller.getConfig().relaxedTimeout(),
        controller.getConfig().relaxedBlockingTimeout(), Long.MAX_VALUE);
  }

  @Override
  public TimeoutInfo getInfo() {
    if (controller.isRebindActive()) {
      return relaxedTimeoutInfo;
    }
    return super.getInfo();
  }
}