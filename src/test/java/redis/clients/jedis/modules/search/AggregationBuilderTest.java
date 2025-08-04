package redis.clients.jedis.modules.search;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.search.aggr.Reducer;
import redis.clients.jedis.search.aggr.Reducers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AggregationBuilderTest {

  @Test
  public void reducerObject() {
    Reducer reducer = Reducers.sum("@count").as("total");
    assertEquals("SUM", reducer.getName());
    assertEquals("@count", reducer.getField());
    assertEquals("total", reducer.getAlias());
  }

  @Test
  public void countObject() {
    Reducer count = Reducers.count();
    assertEquals("COUNT", count.getName());
    assertNull(count.getField());
    assertNull(count.getAlias());
  }
}
