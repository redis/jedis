package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.*;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.EnvCondition;

@Tag("integration")
@Tag("search")
public abstract class SearchDefaultDialectCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> endpoint);

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  protected static final String INDEX = "dialect-INDEX";

  protected static EndpointConfig endpoint;

  protected final RedisProtocol protocol;

  protected UnifiedJedis jedis;

  public SearchDefaultDialectCommandsTestBase(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  protected abstract UnifiedJedis createTestClient();

  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  @BeforeEach
  public void setUp() {
    jedis = createTestClient();
    // Clean up before each test
    try {
      jedis.ftDropIndex(INDEX);
    } catch (Exception e) {
      // Index might not exist, ignore
    }
    jedis.flushAll();
    jedis.setDefaultSearchDialect(SearchProtocol.DEFAULT_DIALECT);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (jedis != null) {
      jedis.close();
    }
  }

  private void addDocument(Document doc) {
    String key = doc.getId();
    Map<String, String> map = new LinkedHashMap<>();
    doc.getProperties().forEach(entry -> map.put(entry.getKey(), String.valueOf(entry.getValue())));
    jedis.hset(key, map);
  }

  @Test
  public void testQueryParams() {
    Schema sc = new Schema().addNumericField("numval");
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    jedis.hset("1", "numval", "1");
    jedis.hset("2", "numval", "2");
    jedis.hset("3", "numval", "3");

    Query query = new Query("@numval:[$min $max]").addParam("min", 1).addParam("max", 2);
    assertEquals(2, jedis.ftSearch(INDEX, query).getTotalResults());
  }

  @Test
  public void testQueryParamsWithParams() {
    assertOK(jedis.ftCreate(INDEX, NumericField.of("numval")));

    jedis.hset("1", "numval", "1");
    jedis.hset("2", "numval", "2");
    jedis.hset("3", "numval", "3");

    assertEquals(2, jedis.ftSearch(INDEX, "@numval:[$min $max]",
        FTSearchParams.searchParams().addParam("min", 1).addParam("max", 2)).getTotalResults());

    Map<String, Object> paramValues = new HashMap<>();
    paramValues.put("min", 1);
    paramValues.put("max", 2);
    assertEquals(2, jedis.ftSearch(INDEX, "@numval:[$min $max]",
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
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    jedis.hset("1", "t1", "hello");

    String q = "(*)";
    Query query = new Query(q).dialect(1);
    assertSyntaxError(query, jedis); // dialect=1 throws syntax error
    query = new Query(q); // dialect=default=2 should return execution plan
    assertThat(jedis.ftExplain(INDEX, query), containsString("WILDCARD"));

    q = "$hello";
    query = new Query(q).dialect(1);
    assertSyntaxError(query, jedis); // dialect=1 throws syntax error
    query = new Query(q).addParam("hello", "hello"); // dialect=default=2 should return execution plan
    assertThat(jedis.ftExplain(INDEX, query), not(emptyOrNullString()));

    q = "@title:(@num:[0 10])";
    query = new Query(q).dialect(1); // dialect=1 should return execution plan
    assertThat(jedis.ftExplain(INDEX, query), not(emptyOrNullString()));
    query = new Query(q); // dialect=default=2
    assertSyntaxError(query, jedis); // dialect=2 throws syntax error

    q = "@t1:@t2:@t3:hello";
    query = new Query(q).dialect(1); // dialect=1 should return execution plan
    assertThat(jedis.ftExplain(INDEX, query), not(emptyOrNullString()));
    query = new Query(q); // dialect=default=2
    assertSyntaxError(query, jedis); // dialect=2 throws syntax error

    q = "@title:{foo}}}}}";
    query = new Query(q).dialect(1); // dialect=1 should return execution plan
    assertThat(jedis.ftExplain(INDEX, query), not(emptyOrNullString()));
    query = new Query(q); // dialect=default=2
    assertSyntaxError(query, jedis); // dialect=2 throws syntax error
  }

  @Test
  public void testAggregationBuilderParamsDialect() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);
    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    Map<String, Object> params = new HashMap<>();
    params.put("name", "abc");

    AggregationBuilder r = new AggregationBuilder("$name")
            .groupBy("@name", Reducers.sum("@count").as("sum"))
            .params(params);

    AggregationResult res = jedis.ftAggregate(INDEX, r);
    assertEquals(1, res.getTotalResults());

    Row r1 = res.getRow(0);
    assertNotNull(r1);
    assertEquals("abc", r1.getString("name"));
    assertEquals(10, r1.getLong("sum"));
  }

  @Test
  public void dialectBoundSpellCheck() {
    jedis.ftCreate(INDEX, TextField.of("t"));
    JedisDataException error = assertThrows(JedisDataException.class,
        () -> jedis.ftSpellCheck(INDEX, "Tooni toque kerfuffle",
            FTSpellCheckParams.spellCheckParams().dialect(0)));
    assertThat(error.getMessage(), containsString("DIALECT requires a non negative integer"));
  }

  private void assertSyntaxError(Query query, UnifiedJedis client) {
    JedisDataException error = assertThrows(JedisDataException.class,
        () -> client.ftExplain(INDEX, query));
    assertThat(error.getMessage(), containsString("Syntax error"));
  }
}

