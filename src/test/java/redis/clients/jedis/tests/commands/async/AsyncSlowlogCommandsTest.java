package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.jedis.tests.commands.async.util.DoNothingCallback;
import redis.clients.util.Slowlog;

import java.util.List;

public class AsyncSlowlogCommandsTest extends AsyncJedisCommandTestBase {

  @Test
  public void slowlog() {
    // do something
    asyncJedis.configSet(new DoNothingCallback<String>(), "slowlog-log-slower-than", "0");
    CommandWithWaiting.set(asyncJedis, "foo", "bar");
    CommandWithWaiting.set(asyncJedis, "foo", "bar2");

    asyncJedis.slowlogGet(SLOWLOG_LIST_CALLBACK.withReset(), 1);
    List<Slowlog> reducedLog = SLOWLOG_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, reducedLog.size());

    Slowlog log = reducedLog.get(0);
    assertTrue(log.getId() > 0);
    assertTrue(log.getTimeStamp() > 0);
    assertTrue(log.getExecutionTime() > 0);
    assertNotNull(log.getArgs());

    asyncJedis.slowlogGetBinary(BYTE_ARRAY_LIST_CALLBACK.withReset(), 1);
    List<byte[]> breducedLog = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, breducedLog.size());

    asyncJedis.slowlogGet(SLOWLOG_LIST_CALLBACK.withReset());
    List<Slowlog> log1 = SLOWLOG_LIST_CALLBACK.getResponseWithWaiting(1000);
    asyncJedis.slowlogGetBinary(BYTE_ARRAY_LIST_CALLBACK.withReset());
    List<byte[]> blog1 = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    assertNotNull(log1);
    assertNotNull(blog1);

    asyncJedis.slowlogLen(LONG_CALLBACK.withReset());
    long len1 = LONG_CALLBACK.getResponseWithWaiting(1000);

    asyncJedis.slowlogReset(new DoNothingCallback<String>());

    asyncJedis.slowlogGet(SLOWLOG_LIST_CALLBACK.withReset());
    List<Slowlog> log2 = SLOWLOG_LIST_CALLBACK.getResponseWithWaiting(1000);
    asyncJedis.slowlogGetBinary(BYTE_ARRAY_LIST_CALLBACK.withReset());
    List<byte[]> blog2 = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    asyncJedis.slowlogLen(LONG_CALLBACK.withReset());
    long len2 = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertTrue(len1 > len2);
    assertTrue(log1.size() > log2.size());
    assertTrue(blog1.size() > blog2.size());
  }
}