package redis.clients.jedis.commands.unified.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.aggr.*;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.EnvCondition;

@Tag("integration")
@Tag("search")
public abstract class AggregateIteratorCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> endpoint);

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  protected static final String INDEX = "aggiteratorindex";

  protected static EndpointConfig endpoint;

  protected final RedisProtocol protocol;

  protected UnifiedJedis jedis;

  public AggregateIteratorCommandsTestBase(RedisProtocol protocol) {
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
  public void testAggregateIteratorBasicUsage() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.hasNext());
      assertNotNull(iterator.getCursorId());

      int totalBatches = 0;
      int totalRows = 0;

      while (iterator.hasNext()) {
        AggregationResult batch = iterator.next();
        assertNotNull(batch);
        assertNotNull(batch.getRows());
        assertTrue(batch.getRows().size() <= 2);
        totalBatches++;
        totalRows += batch.getRows().size();
      }

      assertTrue(totalBatches > 0);
      assertEquals(4, totalRows);
      assertFalse(iterator.hasNext());
    }
  }

  @Test
  public void testAggregateIteratorWithoutCursor() {
    AggregationBuilder aggr = new AggregationBuilder().groupBy("@name",
      Reducers.sum("@count").as("sum"));

    assertThrows(IllegalArgumentException.class, () -> jedis.ftAggregateIterator(INDEX, aggr));
  }

  @Test
  public void testAggregateIteratorSingleBatch() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(100, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.hasNext());

      AggregationResult batch = iterator.next();
      assertNotNull(batch);
      assertNotNull(batch.getRows());
      assertEquals(2, batch.getRows().size());

      assertFalse(iterator.hasNext());
    }
  }

  @Test
  public void testAggregateIteratorFirstBatchReturnsInitialResults() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.hasNext());

      AggregationResult firstBatch = iterator.next();
      assertNotNull(firstBatch);
      assertNotNull(firstBatch.getRows());
      assertEquals(2, firstBatch.getRows().size());

      List<Row> rows = firstBatch.getRows();
      assertEquals("def", rows.get(0).getString("name"));
      assertEquals(30, rows.get(0).getLong("sum"));
      assertEquals("abc", rows.get(1).getString("name"));
      assertEquals(10, rows.get(1).getLong("sum"));

      AggregationResult secondBatch = iterator.next();
      assertEquals(0, secondBatch.getRows().size());
    }
  }

  @Test
  public void testAggregateIteratorEmptyResult() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(10, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.next().isEmpty());
    }
  }

  @Test
  public void testAggregateIteratorRemove() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.hasNext());
      assertNotNull(iterator.getCursorId());
      assertTrue(iterator.getCursorId() > 0);

      AggregationResult firstBatch = iterator.next();
      assertNotNull(firstBatch);
      assertEquals(2, firstBatch.getRows().size());

      assertTrue(iterator.hasNext());

      iterator.remove();

      assertFalse(iterator.hasNext());
      assertEquals(Long.valueOf(-1), iterator.getCursorId());

      assertThrows(NoSuchElementException.class, iterator::next);
    }
  }

  @Test
  public void testAggregateIteratorRemoveBeforeNext() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "cde").set("count", 8));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(1, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.hasNext());
      assertTrue(iterator.getCursorId() > 0);

      iterator.remove();

      assertFalse(iterator.hasNext());
      assertEquals(Long.valueOf(-1), iterator.getCursorId());
    }
  }

  @Test
  public void testAggregateIteratorRemoveAfterClose() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(10, 10000);

    AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr);
    assertTrue(iterator.hasNext());

    iterator.close();

    iterator.remove();
    assertEquals(Long.valueOf(-1), iterator.getCursorId());
  }

  @Test
  public void testAggregateIteratorRemoveMultipleTimes() {
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "cde").set("count", 3));

    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(1, 10000);

    try (AggregateIterator iterator = jedis.ftAggregateIterator(INDEX, aggr)) {
      assertTrue(iterator.hasNext());

      iterator.remove();
      assertFalse(iterator.hasNext());
      assertEquals(-1L, iterator.getCursorId());

      iterator.remove();
      assertEquals(-1L, iterator.getCursorId());
    }
  }
}
