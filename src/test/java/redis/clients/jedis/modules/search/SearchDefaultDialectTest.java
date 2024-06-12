package redis.clients.jedis.modules.search;

import static org.junit.Assert.*;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.*;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;

@RunWith(Parameterized.class)
public class SearchDefaultDialectTest extends RedisModuleCommandsTestBase {

  private static final String INDEX = "dialect-INDEX";
  private static final int DEFAULT_DIALECT = 2;

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public SearchDefaultDialectTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  public void setUp() {
    super.setUp();
    client.setDefaultSearchDialect(DEFAULT_DIALECT);
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  private void addDocument(Document doc) {
    String key = doc.getId();
    Map<String, String> map = new LinkedHashMap<>();
    doc.getProperties().forEach(entry -> map.put(entry.getKey(), String.valueOf(entry.getValue())));
    client.hset(key, map);
  }

  @Test
  public void testQueryParams() {
    Schema sc = new Schema().addNumericField("numval");
    assertEquals("OK", client.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    client.hset("1", "numval", "1");
    client.hset("2", "numval", "2");
    client.hset("3", "numval", "3");

    Query query =  new Query("@numval:[$min $max]").addParam("min", 1).addParam("max", 2);
    assertEquals(2, client.ftSearch(INDEX, query).getTotalResults());
  }

  @Test
  public void testQueryParamsWithParams() {
    assertOK(client.ftCreate(INDEX, NumericField.of("numval")));

    client.hset("1", "numval", "1");
    client.hset("2", "numval", "2");
    client.hset("3", "numval", "3");

    assertEquals(2, client.ftSearch(INDEX, "@numval:[$min $max]",
        FTSearchParams.searchParams().addParam("min", 1).addParam("max", 2)).getTotalResults());

    Map<String, Object> paramValues = new HashMap<>();
    paramValues.put("min", 1);
    paramValues.put("max", 2);
    assertEquals(2, client.ftSearch(INDEX, "@numval:[$min $max]",
        FTSearchParams.searchParams().params(paramValues)).getTotalResults());
  }

  @Test
  public void testDialectsWithFTExplain() throws Exception {
    Map<String, Object> attr = new HashMap<>();
    attr.put("TYPE", "FLOAT32");
    attr.put("DIM", 2);
    attr.put("DISTANCE_METRIC", "L2");

    Schema sc = new Schema()
        .addFlatVectorField("v", attr)
        .addTagField("title")
        .addTextField("t1", 1.0)
        .addTextField("t2", 1.0)
        .addNumericField("num");
    assertEquals("OK", client.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    client.hset("1", "t1", "hello");

    String q = "(*)";
    Query query = new Query(q).dialect(1);
    try {
      client.ftExplain(INDEX, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
    query = new Query(q); // dialect=default=2
    assertTrue("Should contain 'WILDCARD'", client.ftExplain(INDEX, query).contains("WILDCARD"));

    q = "$hello";
    query = new Query(q).dialect(1);
    try {
      client.ftExplain(INDEX, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
    query = new Query(q).addParam("hello", "hello"); // dialect=default=2
    assertTrue("Should contain 'UNION {\n  hello\n  +hello(expanded)\n}\n'",
        client.ftExplain(INDEX, query).contains("UNION {\n  hello\n  +hello(expanded)\n}\n"));

    q = "@title:(@num:[0 10])";
    query = new Query(q).dialect(1);
    assertTrue("Should contain 'NUMERIC {0.000000 <= @num <= 10.000000}'",
        client.ftExplain(INDEX, query).contains("NUMERIC {0.000000 <= @num <= 10.000000}"));
    query = new Query(q); // dialect=default=2
    try {
      client.ftExplain(INDEX, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }

    q = "@t1:@t2:@t3:hello";
    query = new Query(q).dialect(1);
    assertTrue("Should contain '@NULL:UNION {\n  @NULL:hello\n  @NULL:+hello(expanded)\n}\n'",
        client.ftExplain(INDEX, query).contains("@NULL:UNION {\n  @NULL:hello\n  @NULL:+hello(expanded)\n}\n"));
    query = new Query(q); // dialect=default=2
    try {
      client.ftExplain(INDEX, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }

    q = "@title:{foo}}}}}";
    query = new Query(q).dialect(1);
    assertTrue("Should contain 'TAG:@title {\n  foo\n}\n'",
        client.ftExplain(INDEX, query).contains("TAG:@title {\n  foo\n}\n"));
    query = new Query(q); // dialect=default=2
    try {
      client.ftExplain(INDEX, query);
      fail();
    } catch (JedisDataException e) {
      assertTrue("Should contain 'Syntax error'", e.getMessage().contains("Syntax error"));
    }
  }

  @Test
  public void testAggregationBuilderParamsDialect() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    Map<String, Object> params = new HashMap<>();
    params.put("name", "abc");

    AggregationBuilder r = new AggregationBuilder("$name")
            .groupBy("@name", Reducers.sum("@count").as("sum"))
            .params(params);

    AggregationResult res = client.ftAggregate(INDEX, r);
    assertEquals(1, res.getTotalResults());

    Row r1 = res.getRow(0);
    assertNotNull(r1);
    assertEquals("abc", r1.getString("name"));
    assertEquals(10, r1.getLong("sum"));
  }

  @Test
  public void dialectBoundSpellCheck() {
    client.ftCreate(INDEX, TextField.of("t"));
    JedisDataException error = Assert.assertThrows(JedisDataException.class,
        () -> client.ftSpellCheck(INDEX, "Tooni toque kerfuffle",
            FTSpellCheckParams.spellCheckParams().dialect(0)));
    MatcherAssert.assertThat(error.getMessage(), Matchers.containsString("DIALECT requires a non negative integer"));
  }
}
