package redis.clients.jedis.async.callback;

import redis.clients.jedis.exceptions.JedisException;

public interface AsyncResponseCallback<T> {
  public void execute(T response, JedisException exc);
}
