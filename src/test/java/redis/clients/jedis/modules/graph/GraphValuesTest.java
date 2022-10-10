package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.graph.Record;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class GraphValuesTest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  @Test
  public void parseInfinity() {
    ResultSet rs = client.graphQuery("db", "RETURN 10^100000");
    assertEquals(1, rs.size());
    Record r = rs.iterator().next();
    assertEquals(Double.POSITIVE_INFINITY, r.getValue(0), 0d);
  }
}
