package redis.clients.jedis.tests.commands.async.util;

import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.exceptions.JedisException;

public class DoNothingCallback<T> implements AsyncResponseCallback<T> {
  @Override
  public void execute(T response, JedisException exc) {
    // do nothing
  }
}
