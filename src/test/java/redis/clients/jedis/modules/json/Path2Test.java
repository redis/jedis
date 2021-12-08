package redis.clients.jedis.modules.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import redis.clients.jedis.json.Path2;

public class Path2Test {

  @Test(expected = NullPointerException.class)
  public void _null() {
    Path2.of(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void empty() {
    Path2.of("");
  }

  @Test
  public void root() {
    assertEquals("$", Path2.ROOT_PATH.toString());
    assertEquals(Path2.ROOT_PATH, new Path2("$"));
    assertEquals(Path2.ROOT_PATH, Path2.of("$"));
  }

  @Test
  public void test() {
    assertEquals("$.a.b", Path2.of("$.a.b").toString());
    assertEquals("$.a.b", new Path2("$.a.b").toString());
    assertEquals("$.a.b", Path2.of(".a.b").toString());
    assertEquals("$.a.b", new Path2(".a.b").toString());
    assertEquals("$.a.b", Path2.of("a.b").toString());
    assertEquals("$.a.b", new Path2("a.b").toString());
  }

  @Test
  public void equals() {
    assertTrue(new Path2("a.b").equals(Path2.of(".a.b")));
    assertTrue(Path2.of("a.b").equals(new Path2(".a.b")));
  }
}
