package redis.clients.jedis;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Weak-reference registry of pool-managed connections; maintenance marking passes visit it. Registration happens at creation; there is
 * no deregistration — a reference dies with its connection, so destroyed connections can never be
 * pinned by a missed removal. Every live connection is strongly held elsewhere (pool idle queue or
 * borrower), so a live connection is never missed by {@link #forEachLive}; visiting an
 * already-destroyed one is harmless by design (callers only flip an advisory flag, never do I/O).
 * <p>
 * Contents are a conservative over-approximation of liveness refreshed at the GC's convenience —
 * never expose counts as metrics or assert them in tests.
 */
final class ConnectionRegistry {

  private final Set<WeakReference<Connection>> refs = ConcurrentHashMap.newKeySet();
  private final ReferenceQueue<Connection> reaped = new ReferenceQueue<>();

  void register(Connection connection) {
    prune();
    refs.add(new WeakReference<>(connection, reaped));
  }

  /** Visits every still-live registered connection; prunes cleared references on the way. */
  void forEachLive(Consumer<Connection> action) {
    prune();
    for (Iterator<WeakReference<Connection>> it = refs.iterator(); it.hasNext();) {
      Connection connection = it.next().get();
      if (connection == null) {
        it.remove();
      } else {
        action.accept(connection);
      }
    }
  }

  private void prune() {
    Reference<? extends Connection> ref;
    while ((ref = reaped.poll()) != null) {
      refs.remove(ref);
    }
  }
}
