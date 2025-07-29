package redis.clients.jedis;

import java.util.Collection;
import java.util.Collections;

/**
 * A handler object that provides access to {@link PushListener}s.
 * @author Ivo Gaydajiev
 * @since 6.1
 */
public interface PushHandler extends ListenerHandler<PushListener> {

  /**
   * A no-operation implementation of PushHandler that doesn't maintain any listeners
   * <p>
   * All operations are no-ops and getPushListeners() returns an empty list.
   * </p>
   */
  PushHandler NOOP = new NoOpPushHandler();

}

final class NoOpPushHandler implements PushHandler {

  NoOpPushHandler() {
  }

  @Override
  public void addListener(PushListener listener) {
    // No-op
  }

  @Override
  public void removeListener(PushListener listener) {
    // No-op
  }

  @Override
  public void removeAllListeners() {
    // No-op
  }

  @Override
  public Collection<PushListener> getListeners() {
    return Collections.emptyList();
  }
}