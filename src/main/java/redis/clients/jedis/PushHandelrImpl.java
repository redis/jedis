package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class PushHandlerImpl implements PushHandler {
  private final List<PushListener> pushListeners = new CopyOnWriteArrayList<>();

  @Override
  public void addListener(PushListener listener) {
    pushListeners.add(listener);
  }

  @Override
  public void removeListener(PushListener listener) {
    pushListeners.remove(listener);
  }

  @Override
  public Collection<PushListener> getPushListeners() {
    return pushListeners;
  }
}
