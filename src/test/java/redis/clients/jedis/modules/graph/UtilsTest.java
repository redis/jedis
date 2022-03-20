package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import redis.clients.jedis.graph.RedisGraphQueryUtil;

public class UtilsTest {
//
//  @Test
//  public void testPrepareProcedure() {
//    assertEquals("CALL prc()", RedisGraphQueryUtil.prepareProcedure("prc", Arrays.asList(new String[]{}), new HashMap<>()));
//
//    assertEquals("CALL prc(\"a\",\"b\")", RedisGraphQueryUtil.prepareProcedure("prc", Arrays.asList(new String[]{"a", "b"}), new HashMap<>()));
//
//    Map<String, List<String>> kwargs = new HashMap<>();
//    kwargs.put("y", Arrays.asList(new String[]{"ka", "kb"}));
//    assertEquals("CALL prc(\"a\",\"b\")ka,kb", RedisGraphQueryUtil.prepareProcedure("prc", Arrays.asList(new String[]{"a", "b"}), kwargs));
//
//    assertEquals("CALL prc()ka,kb", RedisGraphQueryUtil.prepareProcedure("prc", Arrays.asList(new String[]{}), kwargs));
//  }

  @Test
  public void testParamsPrep() {
    Map<String, Object> params = new HashMap<>();
    params.put("param", "");
    assertEquals("CYPHER param=\"\" RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", "\"");
    assertEquals("CYPHER param=\"\\\"\" RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", "\"st");
    assertEquals("CYPHER param=\"\\\"st\" RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", 1);
    assertEquals("CYPHER param=1 RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", 2.3);
    assertEquals("CYPHER param=2.3 RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", true);
    assertEquals("CYPHER param=true RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", false);
    assertEquals("CYPHER param=false RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", null);
    assertEquals("CYPHER param=null RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", "str");
    assertEquals("CYPHER param=\"str\" RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    params.put("param", "s\"tr");
    assertEquals("CYPHER param=\"s\\\"tr\" RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    Integer arr[] = {1, 2, 3};
    params.put("param", arr);
    assertEquals("CYPHER param=[1, 2, 3] RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    List<Integer> list = Arrays.asList(1, 2, 3);
    params.put("param", list);
    assertEquals("CYPHER param=[1, 2, 3] RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    String strArr[] = {"1", "2", "3"};
    params.put("param", strArr);
    assertEquals("CYPHER param=[\"1\", \"2\", \"3\"] RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
    List<String> stringList = Arrays.asList("1", "2", "3");
    params.put("param", stringList);
    assertEquals("CYPHER param=[\"1\", \"2\", \"3\"] RETURN $param", RedisGraphQueryUtil.prepareQuery("RETURN $param", params));
  }

}
