package redis.clients.jedis;

import java.net.SocketAddress;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.EvictionPolicy;

/**
 * commons-pool2 eviction policy that destroys idle connections whose peer matches the active MOVING
 * rebind's affected node; all other connections are deferred to the wrapped delegate (typically the
 * user's configured policy, defaulting to {@link DefaultEvictionPolicy}).
 */
final class RebindAwareEvictionPolicy implements EvictionPolicy<Connection> {

  private final MaintenanceEventController controller;
  private final EvictionPolicy<Connection> delegate;

  RebindAwareEvictionPolicy(MaintenanceEventController controller,
      EvictionPolicy<Connection> delegate) {
    this.controller = controller;
    this.delegate = (delegate != null) ? delegate : new DefaultEvictionPolicy<Connection>();
  }

  @Override
  public boolean evict(EvictionConfig config, PooledObject<Connection> underTest, int idleCount) {
    if (controller.isAffected(underTest.getObject())) {
      return true;
    }
    return delegate.evict(config, underTest, idleCount);
  }
}
