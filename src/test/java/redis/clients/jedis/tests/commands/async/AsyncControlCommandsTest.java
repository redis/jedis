package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.DebugParams;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.jedis.tests.commands.async.util.DoNothingCallback;

import java.util.List;

public class AsyncControlCommandsTest extends AsyncJedisCommandTestBase {
  @Test
  public void save() {
    try {
      asyncJedis.save(STRING_CALLBACK.withReset());
      assertEquals("OK", STRING_CALLBACK.getResponseWithWaiting(1000));
    } catch (JedisDataException e) {
      assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void bgsave() {
    try {
      asyncJedis.bgsave(STRING_CALLBACK.withReset());
      assertEquals("Background saving started", STRING_CALLBACK.getResponseWithWaiting(1000));
    } catch (JedisDataException e) {
      assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void bgrewriteaof() {
    String scheduled = "Background append only file rewriting scheduled";
    String started = "Background append only file rewriting started";

    asyncJedis.bgrewriteaof(STRING_CALLBACK.withReset());
    String status = STRING_CALLBACK.getResponseWithWaiting(1000);

    boolean ok = status.equals(scheduled) || status.equals(started);
    assertTrue(ok);
  }

  @Test
  public void lastsave() throws InterruptedException {
    asyncJedis.lastsave(LONG_CALLBACK.withReset());
    long saved = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(saved > 0);
  }

  @Test
  public void info() {
    asyncJedis.info(STRING_CALLBACK.withReset());
    String info = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertNotNull(info);

    asyncJedis.info(STRING_CALLBACK.withReset(), "server");
    info = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertNotNull(info);
  }

  @Test
  public void configGet() {
    asyncJedis.configGet(STRING_LIST_CALLBACK.withReset(), "m*");
    List<String> info = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertNotNull(info);
  }

  @Test
  public void configSet() {
    asyncJedis.configGet(STRING_LIST_CALLBACK.withReset(), "maxmemory");
    List<String> info = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    String memory = info.get(1);

    asyncJedis.configSet(STRING_CALLBACK.withReset(), "maxmemory", "200");
    String status = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("OK", status);

    jedis.configSet("maxmemory", memory);
  }

  @Test
  public void debug() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.debug(STRING_CALLBACK.withReset(), DebugParams.OBJECT("foo"));
    String resp = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertNotNull(resp);

    asyncJedis.debug(STRING_CALLBACK.withReset(), DebugParams.RELOAD());
    resp = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertNotNull(resp);
  }

  @Test
  public void waitReplicas() {
    asyncJedis.waitReplicas(LONG_CALLBACK.withReset(), 1, 100);
    Long replicas = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, replicas.longValue());
  }
}