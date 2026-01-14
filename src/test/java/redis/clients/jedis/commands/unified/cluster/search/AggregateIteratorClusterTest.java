package redis.clients.jedis.commands.unified.cluster.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.cluster.ClusterCommandsTestHelper;
import redis.clients.jedis.commands.unified.search.AggregateIteratorBaseTest;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.aggr.AggregateIterator;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;
import redis.clients.jedis.search.aggr.SortedField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cluster (RedisClusterClient) implementation of AggregateIterator tests. Uses cluster-stack
 * endpoint which has RediSearch modules built-in for Redis 8.0+. Requires Redis 8.0+ because
 * earlier versions don't have modules in cluster mode.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class AggregateIteratorClusterTest extends AggregateIteratorBaseTest {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stack");
  }

  public AggregateIteratorClusterTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol, endpoint);
  }

  private void addDocument(Document doc) {
    String key = doc.getId();
    Map<String, String> map = new LinkedHashMap<>();
    doc.getProperties().forEach(entry -> map.put(entry.getKey(), String.valueOf(entry.getValue())));
    jedis.hset(key, map);
  }

  @Test
  public void testAggregateIteratorFirstBatchReturnsInitialResults() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));

    // Create aggregation with cursor that should return 2 results in first batch
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000); // 2 results per batch

    // Test that first next() call returns the initial FT.AGGREGATE results
    try (AggregateIterator iterator = jedis.ftAggregateIterator(index, aggr)) {
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
      assertTrue(secondBatch.isEmpty());
    }
  }

  @Test
  public void testAggregateIteratorBasicUsage() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));
    addDocument(new Document("data3").set("name", "def").set("count", 25));
    addDocument(new Document("data4").set("name", "ghi").set("count", 15));
    addDocument(new Document("data5").set("name", "jkl").set("count", 20));

    // Create aggregation with cursor to test FT.CURSOR routing in cluster mode
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(2, 10000); // 2 results per batch

    // Test the iterator using the integrated method
    try (AggregateIterator iterator = jedis.ftAggregateIterator(index, aggr)) {
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
  public void testAggregateIteratorSingleBatch() {
    // Create index and add test data
    Schema sc = new Schema();
    sc.addSortableTextField("name", 1.0);
    sc.addSortableNumericField("count");
    jedis.ftCreate(index, IndexOptions.defaultOptions(), sc);

    addDocument(new Document("data1").set("name", "abc").set("count", 10));
    addDocument(new Document("data2").set("name", "def").set("count", 5));

    // Create aggregation with large cursor count (all results in one batch)
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@name", Reducers.sum("@count").as("sum")).sortBy(10, SortedField.desc("@sum"))
        .cursor(100, 10000); // Large batch size

    // Test the iterator using the integrated method
    try (AggregateIterator iterator = jedis.ftAggregateIterator(index, aggr)) {
      assertTrue(iterator.hasNext());

      AggregationResult batch = iterator.next();
      assertNotNull(batch);
      assertNotNull(batch.getRows());
      assertEquals(2, batch.getRows().size()); // Should have 2 groups (abc, def)

      // Should be no more batches
      assertFalse(iterator.hasNext());
    }
  }
}
