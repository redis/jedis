package redis.clients.jedis.modules.tairhash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.tairhash.ExhgetWithVerResult;
import redis.clients.jedis.tairhash.ExhincrByParams;
import redis.clients.jedis.tairhash.ExhsetParams;

import static org.junit.Assert.*;

public class TairHashTest extends RedisModuleCommandsTestBase {
  private static final String key = "tairhash";
  private static final String foo = "foo";
  private static final String bar = "bar";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  @Test
  public void exhsetBasic() {
    assertEquals(1, client.exhset(key, foo, bar));
    assertEquals(bar, client.exhget(key, foo));
    assertEquals(1, client.exhdel(key, foo));
  }

  @Test
  public void exhsetWithEx() throws Exception {
    ExhsetParams params = (ExhsetParams) new ExhsetParams().px(500);
    assertEquals(1, client.exhset(key, foo, bar, params));
    Thread.sleep(1000);
    assertEquals(0, client.exhexists(key, foo));
  }

  @Test
  public void exhsetWithNxAndXx() {
    ExhsetParams params = new ExhsetParams().nx();
    assertEquals(1, client.exhset(key, foo, bar, params));
    assertEquals(-1, client.exhset(key, foo, bar, params));
    params = new ExhsetParams().xx();
    assertEquals(0, client.exhset(key, foo, bar, params));
    assertEquals(1, client.exhdel(key, foo));
  }

  @Test
  public void exhsetWithKeepttl() {
    ExhsetParams params = (ExhsetParams) new ExhsetParams().ex(1);
    assertEquals(1, client.exhset(key, foo, bar, params));
    params = (ExhsetParams) new ExhsetParams().keepttl();
    assertEquals(0, client.exhset(key, foo, "newbar", params));
    assertNotEquals(client.pttl(key), 0);
    assertEquals(1, client.exhdel(key, foo));
  }

  @Test
  public void exhsetWithVer() {
    ExhsetParams params = (ExhsetParams) new ExhsetParams().abs(10);
    assertEquals(1, client.exhset(key, foo, bar, params));
    params = (ExhsetParams) new ExhsetParams().ver(9);
    try {
      client.exhset(key, foo, bar, params);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("ERR update version is stale"));
    }
  }

  @Test
  public void exhmsetTest() {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < 5; i++) {
      map.put(i + "", i + "");
    }
    assertEquals("OK", client.exhmset(key, map));
    assertEquals(5, client.exhlen(key));

    Map<String, String> retMap = client.exhgetall(key);
    for (int i = 0; i < 5; i++) {
      assertEquals(i + "", retMap.get(i + ""));
    }
  }

  @Test
  public void exhmgetWithVerTest() {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < 5; i++) {
      map.put(i + "", i + "");
    }
    assertEquals("OK", client.exhmset(key, map));
    assertEquals(5, client.exhlen(key));

    List<ExhgetWithVerResult<String>> results = client.exhmgetWithVer(key, "0", "1", "2", "3", "4");
    for (int i = 0; i < 5; i++) {
      assertEquals(i + "", results.get(i).getValue());
      assertEquals(1, results.get(i).getVer());
    }
  }

  @Test
  public void exhexpireTest() throws Exception {
    assertEquals(1, client.exhset(key, foo, bar));
    assertEquals(1, client.exhexpire(key, foo, 1));
    Thread.sleep(1500);
    assertEquals(0, client.exhexists(key, foo));
  }

  @Test
  public void exhver() {
    assertEquals(1, client.exhset(key, foo, bar));
    assertEquals(1, client.exhver(key, foo));
    assertEquals(1, client.exhsetVer(key, foo, 2));
  }

  @Test
  public void exhincrByTest() throws Exception {
    assertEquals(1, client.exhset(key, foo, "1"));
    assertEquals(2, client.exhincrBy(key, foo, 1));
    ExhincrByParams params = (ExhincrByParams) new ExhincrByParams<>().ex(1);
    assertEquals(3, client.exhincrBy(key, foo, 1, params));
    Thread.sleep(1500);
    assertEquals(0, client.exhexists(key, foo));
  }

  @Test
  public void exhincrByBoundaryTest() {
    assertEquals(1, client.exhset(key, foo, "1"));
    ExhincrByParams<Long> params = (ExhincrByParams<Long>) new ExhincrByParams<Long>()
        .max((long) 2);
    assertEquals(2, client.exhincrBy(key, foo, 1, params));
    try {
      assertEquals(3, client.exhincrBy(key, foo, 1, params));
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("ERR increment or decrement would overflow"));
    }
  }

  @Test
  public void exhdelTest() {
    assertEquals(1, client.exhset(key, foo, bar));
    assertEquals(1, client.exhdel(key, foo));
    assertEquals(0, client.exhexists(key, foo));
  }

  @Test
  public void exhstrlenTest() {
    assertEquals(1, client.exhset(key, foo, bar));
    assertEquals(3, client.exhstrlen(key, foo));
    assertEquals(0, client.exhstrlen("not-exists-key", foo));
    assertEquals(0, client.exhstrlen(key, "not-exists-field"));
  }

  @Test
  public void exhkeysAndValsTest() {
    for (int i = 0; i < 5; i++) {
      assertEquals(1, client.exhset(key, i + "", i + ""));
    }

    List<String> exhkeys = client.exhkeys(key);
    List<String> exhvals = client.exhvals(key);
    Collections.sort(exhkeys);
    Collections.sort(exhvals);
    for (int i = 0; i < 5; i++) {
      assertEquals(i + "", exhkeys.get(i));
      assertEquals(i + "", exhvals.get(i));
    }
  }

  @Test
  public void exhscanTest() {
    for (int i = 0; i < 9; i++) {
      assertEquals(1, client.exhset(key, i + "", i + ""));
    }

    ScanParams params = new ScanParams();
    ScanResult<Entry<String, String>> result = new ScanResult<>("0", null);
    List<String> results = new ArrayList<>();
    do {
      result = client.exhscan(key, result.getCursor(), params);
      for (Map.Entry<String, String> entry : result.getResult()) {
        results.add(entry.getKey());
      }
    } while (!result.getCursor().equals("0"));

    Collections.sort(results);
    for (int i = 0; i < 9; i++) {
      assertEquals(i + "", results.get(i));
    }
  }

}
