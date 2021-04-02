package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import redis.clients.jedis.BuilderFactory;

public class BuilderFactoryTest {
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
  }
}
