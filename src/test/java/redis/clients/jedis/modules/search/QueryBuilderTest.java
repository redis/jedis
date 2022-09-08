package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.search.querybuilder.QueryBuilders.*;
import static redis.clients.jedis.search.querybuilder.Values.*;

import java.util.Arrays;
import org.junit.Test;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.search.querybuilder.Node;
import redis.clients.jedis.search.querybuilder.Value;
import redis.clients.jedis.search.querybuilder.Values;

/**
 * Created by mnunberg on 2/23/18.
 */
public class QueryBuilderTest {

  @Test
  public void testTag() {
    Value v = tags("foo");
    assertEquals("{foo}", v.toString());
    v = tags("foo", "bar");
    assertEquals("{foo | bar}", v.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyTag() {
    tags();
  }

  @Test
  public void testRange() {
    Value v = between(1, 10);
    assertEquals("[1 10]", v.toString());
    v = between(1, 10).inclusiveMax(false);
    assertEquals("[1 (10]", v.toString());
    v = between(1, 10).inclusiveMin(false);
    assertEquals("[(1 10]", v.toString());

    v = between(1.0, 10.1);
    assertEquals("[1.0 10.1]", v.toString());
    v = between(-1.0, 10.1).inclusiveMax(false);
    assertEquals("[-1.0 (10.1]", v.toString());
    v = between(-1.1, 150.61).inclusiveMin(false);
    assertEquals("[(-1.1 150.61]", v.toString());

    // le, gt, etc.
    // le, gt, etc.
    assertEquals("[42 42]", eq(42).toString());
    assertEquals("[-inf (42]", lt(42).toString());
    assertEquals("[-inf 42]", le(42).toString());
    assertEquals("[(-42 inf]", gt(-42).toString());
    assertEquals("[42 inf]", ge(42).toString());

    assertEquals("[42.0 42.0]", eq(42.0).toString());
    assertEquals("[-inf (42.0]", lt(42.0).toString());
    assertEquals("[-inf 42.0]", le(42.0).toString());
    assertEquals("[(42.0 inf]", gt(42.0).toString());
    assertEquals("[42.0 inf]", ge(42.0).toString());

    assertEquals("[(1587058030 inf]", gt(1587058030).toString());

    // string value
    assertEquals("s", value("s").toString());

    // Geo value
    assertEquals("[1.0 2.0 3.0 km]",
        geo(new GeoCoordinate(1.0, 2.0), 3.0, GeoUnit.KM).toString());
  }

  @Test
  public void testIntersectionBasic() {
    Node n = intersect().add("name", "mark");
    assertEquals("@name:mark", n.toString());

    n = intersect().add("name", "mark", "dvir");
    assertEquals("@name:(mark dvir)", n.toString());

    n = intersect().add("name", Arrays.asList(Values.value("mark"), Values.value("shay")));
    assertEquals("@name:(mark shay)", n.toString());

    n = intersect("name", "meir");
    assertEquals("@name:meir", n.toString());

    n = intersect("name", Values.value("meir"), Values.value("rafi"));
    assertEquals("@name:(meir rafi)", n.toString());
  }

  @Test
  public void testIntersectionNested() {
    Node n = intersect().
        add(union("name", value("mark"), value("dvir"))).
        add("time", between(100, 200)).
        add(disjunct("created", lt(1000)));
    assertEquals("(@name:(mark|dvir) @time:[100 200] -@created:[-inf (1000])", n.toString());
  }

  @Test
  public void testOptional() {
    Node n = optional("name", tags("foo", "bar"));
    assertEquals("~@name:{foo | bar}", n.toString());

    n = optional(n, n);
    assertEquals("~(~@name:{foo | bar} ~@name:{foo | bar})", n.toString());
  }
}
