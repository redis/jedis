package redis.clients.jedis.mcf;

import static redis.clients.jedis.mcf.MultiDbConnectionProviderHelper.activeConnectionPool;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.aggr.AggregateIterator;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.Row;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.util.TestKeyRegistry;

/**
 * Integration tests for {@code ftAggregateIteration} and {@code ftAggregateIterator} on a
 * {@link MultiDbClient}: the pooled connection is returned to the pool once the iteration ends.
 */
public class MultiDbAggregateIterationIT {

  private static EndpointConfig endpoint;
  private MultiDbClient client;
  private TestKeyRegistry keys;
  private String index;

  @BeforeAll
  public static void prepareEndpoints() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  @BeforeEach
  public void setUp(TestInfo testInfo) {
    keys = TestKeyRegistry.create(testInfo);
    DatabaseConfig db = DatabaseConfig
        .builder(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build()).weight(1.0f)
        .build();
    client = MultiDbClient.builder()
        .multiDbConfig(new MultiDbConfig.Builder(new DatabaseConfig[] { db }).build()).build();

    index = keys.name("idx");
    client.ftCreate(index,
      FTCreateParams.createParams().prefix(keys.name("doc:")).skipInitialScan(),
      Arrays.asList(TextField.of("name").sortable(), NumericField.of("count")));
    for (int i = 0; i < 5; i++) {
      Map<String, String> doc = new HashMap<>();
      doc.put("name", "name" + i);
      doc.put("count", String.valueOf(i));
      client.hset(keys.key("doc:" + i), doc);
    }
  }

  @AfterEach
  public void tearDown() {
    if (client != null) {
      try {
        client.ftDropIndex(index);
      } catch (Exception ignore) {
      }
      try {
        keys.cleanup(client);
      } catch (Exception ignore) {
      }
      client.close();
    }
  }

  private static AggregationBuilder aggregation() {
    return new AggregationBuilder().groupBy("@name", Reducers.sum("@count").as("sum")).cursor(2,
      10000);
  }

  /** Regression test for #4799: ftAggregateIteration must return its pooled connection. */
  @Test
  public void ftAggregateIteration_releasesConnection() {
    ConnectionPool pool = activeConnectionPool(client);

    List<Row> rows = new ArrayList<>(
        client.ftAggregateIteration(index, aggregation()).collect(new ArrayList<>()));

    assertEquals(5, rows.size(), "iteration must run to completion");
    assertEquals(0, pool.getNumActive(), "iteration must not keep a pooled connection");
  }

  /** Regression test for #4799: ftAggregateIterator must return its pooled connection. */
  @Test
  public void ftAggregateIterator_releasesConnection() {
    ConnectionPool pool = activeConnectionPool(client);

    int rows = 0;
    try (AggregateIterator iterator = client.ftAggregateIterator(index, aggregation())) {
      while (iterator.hasNext()) {
        rows += iterator.next().getRows().size();
      }
    }

    assertEquals(5, rows, "iteration must run to completion");
    assertEquals(0, pool.getNumActive(), "iterator must not keep a pooled connection");
  }
}
