package redis.clients.jedis.tests.commands.async;

import org.junit.After;
import org.junit.Before;
import org.junit.ComparisonFailure;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.async.AsyncJedis;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.JedisTestBase;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.jedis.tests.commands.async.util.AsyncJUnitTestCallback;
import redis.clients.jedis.tests.commands.async.util.DoNothingCallback;
import redis.clients.util.Slowlog;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AsyncJedisCommandTestBase extends JedisCommandTestBase {
  protected AsyncJedis asyncJedis;
  protected final Lock lock = new ReentrantLock(true);

  public AsyncJedisCommandTestBase() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    asyncJedis = new AsyncJedis(hnp.getHost(), hnp.getPort(), "foobared");
    asyncJedis.configSet(new DoNothingCallback<String>(), "timeout", "300");

    jedis.flushAll();
  }

  @After
  public void tearDown() {
    super.tearDown();

    try {
      asyncJedis.stop();
    } catch (InterruptedException e) {
      // FIXME : pass or fail?
    }
  }

  protected final AsyncJUnitTestCallback<Long> LONG_CALLBACK = new AsyncJUnitTestCallback<Long>();
  protected final AsyncJUnitTestCallback<String> STRING_CALLBACK = new AsyncJUnitTestCallback<String>();
  protected final AsyncJUnitTestCallback<byte[]> BYTE_ARRAY_CALLBACK = new AsyncJUnitTestCallback<byte[]>();
  protected final AsyncJUnitTestCallback<Boolean> BOOLEAN_CALLBACK = new AsyncJUnitTestCallback<Boolean>();
  protected final AsyncJUnitTestCallback<List<Boolean>> BOOLEAN_LIST_CALLBACK = new AsyncJUnitTestCallback<List<Boolean>>();
  protected final AsyncJUnitTestCallback<Double> DOUBLE_CALLBACK = new AsyncJUnitTestCallback<Double>();
  protected final AsyncJUnitTestCallback<Set<String>> STRING_SET_CALLBACK = new AsyncJUnitTestCallback<Set<String>>();
  protected final AsyncJUnitTestCallback<List<String>> STRING_LIST_CALLBACK = new AsyncJUnitTestCallback<List<String>>();
  protected final AsyncJUnitTestCallback<Set<byte[]>> BYTE_ARRAY_SET_CALLBACK = new AsyncJUnitTestCallback<Set<byte[]>>();
  protected final AsyncJUnitTestCallback<List<byte[]>> BYTE_ARRAY_LIST_CALLBACK = new AsyncJUnitTestCallback<List<byte[]>>();
  protected final AsyncJUnitTestCallback<List<Slowlog>> SLOWLOG_LIST_CALLBACK = new AsyncJUnitTestCallback<List<Slowlog>>();
  protected final AsyncJUnitTestCallback<Set<Tuple>> TUPLE_SET_CALLBACK = new AsyncJUnitTestCallback<Set<Tuple>>();
  protected final AsyncJUnitTestCallback<Set<Tuple>> TUPLE_BINARY_SET_CALLBACK = new AsyncJUnitTestCallback<Set<Tuple>>();
  protected final AsyncJUnitTestCallback<Map<String, String>> STRING_MAP_CALLBACK = new AsyncJUnitTestCallback<Map<String, String>>();
  protected final AsyncJUnitTestCallback<Map<byte[], byte[]>> BYTE_ARRAY_MAP_CALLBACK = new AsyncJUnitTestCallback<Map<byte[], byte[]>>();
  protected final AsyncJUnitTestCallback<Object> OBJECT_CALLBACK = new AsyncJUnitTestCallback<Object>();
}
