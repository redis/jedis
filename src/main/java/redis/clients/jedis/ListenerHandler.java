package redis.clients.jedis;

import java.util.Collection;

public interface ListenerHandler<T> {
  void addListener(T listener);

  void removeListener(T listener);

  void removeAllListeners();

  Collection<T> getListeners();

}