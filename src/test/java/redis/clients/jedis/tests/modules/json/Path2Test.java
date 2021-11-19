package redis.clients.jedis.tests.modules.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import redis.clients.jedis.json.Path2;

public class Path2Test {

  @Test
  public void test() {
    assertEquals("$", Path2.ROOT_PATH.toString());
    assertEquals("$.a.b", Path2.of("$.a.b").toString());
    assertEquals("$.a.b", new Path2("$.a.b").toString());
    assertEquals("$.a.b", Path2.of(".a.b").toString());
    assertEquals("$.a.b", new Path2(".a.b").toString());
    assertEquals("$.a.b", Path2.of("a.b").toString());
    assertEquals("$.a.b", new Path2("a.b").toString());
  }
}
