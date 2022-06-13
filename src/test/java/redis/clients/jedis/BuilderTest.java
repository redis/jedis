package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.BuilderFactory.DOUBLE;

import org.junit.Assert;
import org.junit.Test;

public class BuilderTest {

  @Test
  public void buildDouble() {
    Double build = BuilderFactory.DOUBLE.build("1.0".getBytes());
    assertEquals(Double.valueOf(1.0), build);
    build = BuilderFactory.DOUBLE.build("inf".getBytes());
    assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), build);
    build = BuilderFactory.DOUBLE.build("+inf".getBytes());
    assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), build);
    build = BuilderFactory.DOUBLE.build("-inf".getBytes());
    assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), build);

    try {
      build = DOUBLE.build("".getBytes());
      Assert.fail("testDouble should have thrown NumberFormatException");
    } catch (NumberFormatException expected) {
      Assert.assertEquals("empty String", expected.getMessage());
    }
  }
}
