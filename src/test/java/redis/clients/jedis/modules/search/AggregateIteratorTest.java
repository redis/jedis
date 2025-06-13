package redis.clients.jedis.modules.search;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;

import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.aggr.*;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class AggregateIteratorTest {

  private static final String index = "aggiteratorindex";
  private static final String address = System.getProperty("modulesDocker",
    Protocol.DEFAULT_HOST + ':' + 6479);
  private static final HostAndPort hnp = HostAndPort.from(address);

  private final RedisProtocol protocol;
  private Jedis jedis;
  private UnifiedJedis client;

  public AggregateIteratorTest(RedisProtocol redisProtocol) {
    this.protocol = redisProtocol;
  }

  @BeforeEach
  public void setUp() {
    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().protocol(protocol).build());
    jedis.flushAll();
    client = new UnifiedJedis(hnp, DefaultJedisClientConfig.builder().protocol(protocol).build());
  }

  @AfterEach
  public void tearDown() throws Exception {
    client.close();
    jedis.close();
  }

  private void addDocument(Document doc) {
    String key = doc.getId();
    Map<String, String> map = new LinkedHashMap<>();
    doc.getProperties().forEach(entry -> map.put(entry.getKey(), String.valueOf(entry.getValue())));
    client.hset(key, map);
  }

  @Test
  public void testAggregateIteratorBasicUsage() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    // Create aggregation with cursor
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000); // 2 results per batch

    // Test the iterator using the integrated method
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());
      assertNotNull(iterator.getCursorId());

      int totalBatches = 0;
      int totalRows = 0;

      while (iterator.hasNext()) {
        AggregationResult batch = iterator.next();
        assertNotNull(batch);
        assertNotNull(batch.getRows());
        assertTrue(batch.getRows().size() <= 2); // Batch size should not exceed cursor count
        totalBatches++;
        totalRows += batch.getRows().size();
      }

      assertTrue(totalBatches > 0);
      assertEquals(4, totalRows); // Should have 4 groups total (abc, def, ghi, jkl)
      assertFalse(iterator.hasNext());
    }
  }

  @Test
  @SuppressWarnings("resource") // Expected to throw exception before resource is created
  public void testAggregateIteratorWithoutCursor() {
    // Create aggregation without cursor - should throw exception
    AggregationBuilder aggr = new AggregationBuilder().groupBy("@name",
      Reducers.sum("@count").as("sum"));

    assertThrows(IllegalArgumentException.class, () -> {
      client.ftAggregateIterator(index, aggr);
    });
  }

  @Test
  public void testAggregateIteratorSingleBatch() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));

    // Create aggregation with large cursor count (all results in one batch)
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(100, 10000); // Large batch size

    // Test the iterator using the integrated method
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());

      AggregationResult batch = iterator.next();
      assertNotNull(batch);
      assertNotNull(batch.getRows());
      assertEquals(2, batch.getRows().size()); // Should have 2 groups (abc, def)

      // Should be no more batches
      assertFalse(iterator.hasNext());
    }
  }

  @Test
  public void testAggregateIteratorFirstBatchReturnsInitialResults() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    // Create aggregation with cursor that should return 2 results in first batch
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000); // 2 results per batch

    // Test that first next() call returns the initial FT.AGGREGATE results
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());

      // First call should return initial results from FT.AGGREGATE
      AggregationResult firstBatch = iterator.next();
      assertNotNull(firstBatch);
      assertNotNull(firstBatch.getRows());
      assertEquals(2, firstBatch.getRows().size()); // Should have 2 groups (abc, def)

      // Verify the results are correct
      List<Row> rows = firstBatch.getRows();
      assertEquals("def", rows.get(0).getString("name"));
      assertEquals(30, rows.get(0).getLong("sum"));
      assertEquals("abc", rows.get(1).getString("name"));
      assertEquals(10, rows.get(1).getLong("sum"));

      // Should be no more batches since we got all results in first batch
      AggregationResult secondBatch = iterator.next();
      assertEquals(0, secondBatch.getRows().size());
    }
  }

  @Test
  public void testAggregateIteratorEmptyResult() {
    // Create index but add no data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    // Create aggregation with cursor
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(10, 10000);

    // Test the iterator with empty results using the integrated method
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      // Should have no results
      assertTrue(iterator.next().isEmpty());
    }
  }

  @Test
  public void testAggregateIteratorRemove() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    // Create aggregation with cursor
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000); // 2 results per batch

    // Test remove() method
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());
      assertNotNull(iterator.getCursorId());
      assertTrue(iterator.getCursorId() > 0);

      // Get first batch
      AggregationResult firstBatch = iterator.next();
      assertNotNull(firstBatch);
      assertEquals(2, firstBatch.getRows().size());

      // Should still have more results
      assertTrue(iterator.hasNext());

      // Remove the cursor - this should terminate the iteration
      iterator.remove();

      // After remove, should have no more results
      assertFalse(iterator.hasNext());
      assertEquals(Long.valueOf(-1), iterator.getCursorId());

      // Calling next() should throw NoSuchElementException
      assertThrows(NoSuchElementException.class, iterator::next);
    }
  }

  @Test
  public void testAggregateIteratorRemoveBeforeNext() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "cde").set("count", 8));

    // Create aggregation with cursor
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(1, 10000);

    // Test calling remove() before next() - should work since cursor is initialized
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());
      assertTrue(iterator.getCursorId() > 0);

      // Remove without calling next() first
      iterator.remove();

      // After remove, should have no more results
      assertFalse(iterator.hasNext());
      assertEquals(Long.valueOf(-1), iterator.getCursorId());
    }
  }

  @Test
  public void testAggregateIteratorRemoveAfterClose() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));

    // Create aggregation with cursor
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(10, 10000);

    AggregateIterator iterator = client.ftAggregateIterator(index, aggr);
    assertTrue(iterator.hasNext());

    // Close the iterator
    iterator.close();

    // Calling remove() after close should not throw exception, just return silently
    iterator.remove(); // Should not throw
    assertEquals(Long.valueOf(-1), iterator.getCursorId());
  }

  @Test
  public void testAggregateIteratorRemoveMultipleTimes() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    client.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "cde").set("count", 3));

    // Create aggregation with cursor
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).cursor(1, 10000);

    // Test calling remove() multiple times
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());

      // First remove should work
      iterator.remove();
      assertFalse(iterator.hasNext());
      assertEquals(-1L, iterator.getCursorId());

      // Second remove should not throw exception, just return silently
      iterator.remove(); // Should not throw
      assertEquals(-1L, iterator.getCursorId());
    }
  }
}
