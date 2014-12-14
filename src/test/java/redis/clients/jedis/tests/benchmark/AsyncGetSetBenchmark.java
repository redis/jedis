package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.async.AsyncJedis;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.commands.async.util.AsyncJUnitTestCallback;

public class AsyncGetSetBenchmark {
  private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
  private static final int TOTAL_OPERATIONS = 1000000;

  public static void main(String[] args) throws UnknownHostException, IOException,
      InterruptedException {
    AsyncJedis jedis = new AsyncJedis(hnp.getHost(), hnp.getPort(), "foobared");
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    jedis.flushAll(callback);
    callback.getResponseWithWaiting(10000);

    long begin = Calendar.getInstance().getTimeInMillis();

    ResponseCounterCallback<String> setCounterCallback = new ResponseCounterCallback<String>();
    ResponseCounterCallback<String> getCounterCallback = new ResponseCounterCallback<String>();

    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      String key = "foo" + n;
      jedis.set(setCounterCallback, key, "bar" + n);
      jedis.get(getCounterCallback, key);
    }

    jedis.close();

    System.out.println("set counter : " + setCounterCallback.getCount().get() + " / "
        + "get counter : " + getCounterCallback.getCount().get());

    long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

    System.out.println((TOTAL_OPERATIONS / elapsed * 1000 * 2) + " ops");
  }

  public static class ResponseCounterCallback<T> implements AsyncResponseCallback<T> {
    private volatile AtomicInteger count = new AtomicInteger(0);

    @Override
    public void execute(T response, JedisException exc) {
      if (exc != null) {
        System.err.println("Exception occurred : " + exc);
      } else {
        count.incrementAndGet();
      }
    }

    public AtomicInteger getCount() {
      return count;
    }

  }
}