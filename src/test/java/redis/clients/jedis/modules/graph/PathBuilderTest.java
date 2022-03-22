package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import redis.clients.jedis.graph.entities.Edge;

public class PathBuilderTest {

  @Test
  public void testPathBuilderSizeException() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      PathBuilder builder = new PathBuilder(0);
      builder.build();
    });
    assertTrue(exception.getMessage().equalsIgnoreCase("Path builder nodes count should be edge count + 1"));
  }

  @Test
  public void testPathBuilderArgumentsException() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      PathBuilder builder = new PathBuilder(0);
      builder.append(new Edge());
    });
    assertTrue(exception.getMessage().equalsIgnoreCase("Path Builder expected Node but was Edge"));
  }
}
