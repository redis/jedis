package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.EvictionPolicy;

/**
 * commons-pool2 eviction policy that destroys idle connections marked by a maintenance marking
 * pass. All other connections are deferred to the wrapped delegate (typically the user's configured
 * policy, defaulting to {@link DefaultEvictionPolicy}).
 */
final class RebindAwareEvictionPolicy implements EvictionPolicy<Connection> {

  private final EvictionPolicy<Connection> delegate;

  RebindAwareEvictionPolicy(EvictionPolicy<Connection> delegate) {
    this.delegate = (delegate != null) ? delegate : new DefaultEvictionPolicy<Connection>();
  }

  @Override
  public boolean evict(EvictionConfig config, PooledObject<Connection> underTest, int idleCount) {
    if (underTest.getObject().isMarkedForReconnect()) {
      return true;
    }
    return delegate.evict(config, underTest, idleCount);
  }
}
