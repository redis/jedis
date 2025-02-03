package redis.clients.jedis.modules.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static redis.clients.jedis.util.AssertUtil.assertEqualsByProtocol;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.*;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TagField;
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

  private static Map<String, String> toMap(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
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
    assertSyntaxError(query, client); // dialect=1 throws syntax error
    query = new Query(q); // dialect=default=2 should return execution plan
    assertThat(client.ftExplain(INDEX, query), containsString("WILDCARD"));

    q = "$hello";
    query = new Query(q).dialect(1);
    assertSyntaxError(query, client); // dialect=1 throws syntax error
    query = new Query(q).addParam("hello", "hello"); // dialect=default=2 should return execution plan
    assertThat(client.ftExplain(INDEX, query), not(emptyOrNullString()));


    q = "@title:(@num:[0 10])";
    query = new Query(q).dialect(1); // dialect=1 should return execution plan
    assertThat(client.ftExplain(INDEX, query), not(emptyOrNullString()));
    query = new Query(q); // dialect=default=2
    assertSyntaxError(query, client); // dialect=2 throws syntax error

    q = "@t1:@t2:@t3:hello";
    query = new Query(q).dialect(1); // dialect=1 should return execution plan
    assertThat(client.ftExplain(INDEX, query), not(emptyOrNullString()));
    query = new Query(q); // dialect=default=2
    assertSyntaxError(query, client); // dialect=2 throws syntax error

    q = "@title:{foo}}}}}";
    query = new Query(q).dialect(1); // dialect=1 should return execution plan
    assertThat(client.ftExplain(INDEX, query), not(emptyOrNullString()));
    query = new Query(q); // dialect=default=2
    assertSyntaxError(query, client); // dialect=2 throws syntax error
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
    JedisDataException error = assertThrows(JedisDataException.class,
        () -> client.ftSpellCheck(INDEX, "Tooni toque kerfuffle",
            FTSpellCheckParams.spellCheckParams().dialect(0)));
    assertThat(error.getMessage(), containsString("DIALECT requires a non negative integer"));
  }

  private void assertSyntaxError(Query query, UnifiedJedis client) {
    JedisDataException error = assertThrows(JedisDataException.class,
        () -> client.ftExplain(INDEX, query));
    assertThat(error.getMessage(), containsString("Syntax error"));
  }

  @Test
  @SinceRedisVersion(value = "7.9.0")
  public void warningMaxPrefixExpansions() {
    final String configParam = "search-max-prefix-expansions";
    String defaultConfigValue = jedis.configGet(configParam).get(configParam);
    try {
      assertOK(client.ftCreate(INDEX, FTCreateParams.createParams().on(IndexDataType.HASH),
          TextField.of("t"), TagField.of("t2")));

      client.hset("doc13", toMap("t", "foo", "t2", "foo"));

      jedis.configSet(configParam, "1");

      SearchResult srcResult = client.ftSearch(INDEX, "fo*");
      assertEqualsByProtocol(protocol, null, Arrays.asList(), srcResult.getWarnings());

      client.hset("doc23", toMap("t", "fooo", "t2", "fooo"));

      AggregationResult aggResult = client.ftAggregate(INDEX, new AggregationBuilder("fo*").loadAll());
      assertEqualsByProtocol(protocol,
          /* resp2 */ null,
          Arrays.asList("Max prefix expansions limit was reached"),
          aggResult.getWarnings());
    } finally {
      jedis.configSet(configParam, defaultConfigValue);
    }
  }

}
