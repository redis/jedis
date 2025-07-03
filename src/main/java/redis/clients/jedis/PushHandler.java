package redis.clients.jedis;

import java.util.Collection;
import java.util.Collections;

/**
 * A handler object that provides access to {@link PushListener}.
 *
 * @author Ivo Gaydajiev
 * @since 6.1
 */
public interface PushHandler {

  /**
   * Add a new {@link PushListener listener}.
   *
   * @param listener the listener, must not be {@code null}.
   */
  void addListener(PushListener listener);

  /**
   * Remove an existing {@link PushListener listener}.
   *
   * @param listener the listener, must not be {@code null}.
   */
  void removeListener(PushListener listener);

  /**
   * Returns a collection of {@link PushListener}.
   *
   * @return the collection of listeners.
   */
  Collection<PushListener> getPushListeners();

  /**
   * A no-operation implementation of PushHandler that doesn't maintain any listeners.
   * All operations are no-ops and getPushListeners() returns an empty list.
   * Implemented as a singleton to avoid unnecessary object creation.
   */
  PushHandler NOOP = new NoOpPushHandler();
}

/**
 * Singleton implementation of a no-operation PushHandler.
 */
final class NoOpPushHandler implements PushHandler {

  // Package-private constructor to prevent external instantiation
  NoOpPushHandler() {}

  @Override
  public void addListener(PushListener listener) {
    // No-op
  }

  @Override
  public void removeListener(PushListener listener) {
    // No-op
  }

  @Override
  public Collection<PushListener> getPushListeners() {
    return Collections.emptyList();
  }
}