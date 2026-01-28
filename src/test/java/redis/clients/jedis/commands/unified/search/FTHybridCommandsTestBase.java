package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.search.hybrid.FTHybridCombineParams;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.hybrid.FTHybridParams;
import redis.clients.jedis.search.hybrid.HybridResult;
import redis.clients.jedis.search.hybrid.FTHybridSearchParams;
import redis.clients.jedis.search.hybrid.FTHybridVectorParams;
import redis.clients.jedis.search.IndexDataType;
import redis.clients.jedis.search.hybrid.FTHybridPostProcessingParams;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.TagField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.search.schemafields.VectorField;

/**
 * Base test class for FT.HYBRID command using the UnifiedJedis pattern. Tests hybrid search
 * functionality combining text search and vector similarity.
 */
@Tag("integration")
@Tag("search")
@SinceRedisVersion("8.4.0")
public abstract class FTHybridCommandsTestBase extends UnifiedJedisCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  private static final String INDEX_NAME = "hybrid-test-idx";
  private static final String PREFIX = "product:hybrid:";
  private static final int VECTOR_DIM = 10;

  protected byte[] queryVector;

  public FTHybridCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void setUpTestData() {
    if (!jedis.ftList().contains(INDEX_NAME)) {
      createHybridIndex();
      createSampleProducts();
    }

    // Always initialize query vector (cheap operation)
    queryVector = floatArrayToByteArray(
      new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
  }

  // ========== Setup Helper Methods ==========

  private void createHybridIndex() {
    Map<String, Object> vectorAttrs = new HashMap<>();
    vectorAttrs.put("TYPE", "FLOAT32");
    vectorAttrs.put("DIM", VECTOR_DIM);
    vectorAttrs.put("DISTANCE_METRIC", "COSINE");

    assertOK(jedis.ftCreate(INDEX_NAME,
      FTCreateParams.createParams().on(IndexDataType.HASH).prefix(PREFIX), TextField.of("title"),
      TagField.of("category"), TagField.of("brand"), NumericField.of("price"),
      NumericField.of("rating"), VectorField.builder().fieldName("image_embedding")
          .algorithm(VectorField.VectorAlgorithm.HNSW).attributes(vectorAttrs).build()));
  }

  private void createSampleProducts() {
    // Electronics - Apple products
    createProduct("1", "Apple iPhone 15 Pro smartphone with advanced camera", "electronics",
      "apple", "999", "4.8",
      new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
    createProduct("4", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple", "999",
      "4.8", new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
    createProduct("10", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple", "999",
      "4.8", new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });

    // Electronics - Samsung products
    createProduct("2", "Samsung Galaxy S24 smartphone camera", "electronics", "samsung", "799",
      "4.6", new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f, 0.9f });
    createProduct("5", "Samsung Galaxy S24", "electronics", "samsung", "799", "4.6",
      new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f, 0.9f });

    // Electronics - Google products
    createProduct("3", "Google Pixel 8 Pro camera smartphone", "electronics", "google", "699",
      "4.5", new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f });
    createProduct("6", "Google Pixel 8 Pro", "electronics", "google", "699", "4.5",
      new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f });

    // Other categories
    createProduct("7", "Best T-shirt", "apparel", "denim", "255", "4.2",
      new float[] { 0.12f, 0.22f, 0.32f, 0.42f, 0.52f, 0.62f, 0.72f, 0.82f, 0.92f, 0.85f });
    createProduct("8", "Best makeup", "beauty", "loreal", "155", "4.4",
      new float[] { 0.18f, 0.28f, 0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.88f, 0.98f, 0.75f });
    createProduct("9", "Best punching bag", "sports", "lonsdale", "733", "4.6",
      new float[] { 0.11f, 0.21f, 0.31f, 0.41f, 0.51f, 0.61f, 0.71f, 0.81f, 0.91f, 0.95f });
  }

  private void createProduct(String id, String title, String category, String brand, String price,
      String rating, float[] embedding) {
    Map<byte[], byte[]> fields = new HashMap<>();
    fields.put("title".getBytes(), title.getBytes());
    fields.put("category".getBytes(), category.getBytes());
    fields.put("brand".getBytes(), brand.getBytes());
    fields.put("price".getBytes(), price.getBytes());
    fields.put("rating".getBytes(), rating.getBytes());
    fields.put("image_embedding".getBytes(), floatArrayToByteArray(embedding));

    jedis.hset((PREFIX + id).getBytes(), fields);
  }

  private byte[] floatArrayToByteArray(float[] floats) {
    ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (float f : floats) {
      buffer.putFloat(f);
    }
    return buffer.array();
  }

  // ========== Test Methods ==========

  @Test
  public void testComprehensiveFtHybridWithAllFeatures() {
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("price", "brand", "@category")
        .groupBy(FTHybridPostProcessingParams.GroupBy.of("brand")
            .reduce(FTHybridPostProcessingParams.Reducer
                .of(FTHybridPostProcessingParams.ReduceFunction.SUM, "@price").as("sum"))
            .reduce(FTHybridPostProcessingParams.Reducer
                .of(FTHybridPostProcessingParams.ReduceFunction.COUNT).as("count")))
        .apply(FTHybridPostProcessingParams.Apply.of("@sum * 0.9", "discounted_price"))
        .sortBy(FTHybridPostProcessingParams.SortBy.of(
          new FTHybridPostProcessingParams.SortProperty("sum",
              FTHybridPostProcessingParams.SortDirection.ASC),
          new FTHybridPostProcessingParams.SortProperty("count",
              FTHybridPostProcessingParams.SortDirection.DESC)))
        .filter(FTHybridPostProcessingParams.Filter.of("@sum > 700"))
        .limit(FTHybridPostProcessingParams.Limit.of(0, 20)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics} smartphone camera")
            .scorer(FTHybridSearchParams.Scorer.of("BM25")).scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(20).efRuntime(150))
            .filter("(@brand:{apple|samsung|google}) (@price:[500 1500]) (@category:{electronics})")
            .scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams
            .of(new FTHybridCombineParams.Linear().alpha(0.7).beta(0.3).window(25)))
        .postProcessing(postProcessing).param("discount_rate", "0.9").param("vector", queryVector)
        .build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));
    assertThat(reply.getTotalResults(), equalTo(3L));
    assertThat(reply.getResults().size(), equalTo(3));
    assertThat(reply.getWarnings().size(), greaterThanOrEqualTo(0));
    assertThat(reply.getExecutionTime(), greaterThan(0.0));

    // Verify first result (google) - exact field values
    Map<String, Object> r1 = reply.getResults().get(0);
    assertThat(r1.get("brand"), equalTo("google"));
    assertThat(r1.get("count"), equalTo("2"));
    assertThat(r1.get("sum"), equalTo("1398"));
    assertThat(r1.get("discounted_price"), equalTo("1258.2"));

    // Verify second result (samsung) - exact field values
    Map<String, Object> r2 = reply.getResults().get(1);
    assertThat(r2.get("brand"), equalTo("samsung"));
    assertThat(r2.get("count"), equalTo("2"));
    assertThat(r2.get("sum"), equalTo("1598"));
    assertThat(r2.get("discounted_price"), equalTo("1438.2"));

    // Verify third result (apple) - exact field values
    Map<String, Object> r3 = reply.getResults().get(2);
    assertThat(r3.get("brand"), equalTo("apple"));
    assertThat(r3.get("count"), equalTo("3"));
    assertThat(r3.get("sum"), equalTo("2997"));
    assertThat(r3.get("discounted_price"), equalTo("2697.3"));
  }

  @Test
  public void testLoadSpecificFields() {
    // Test LOAD with specific fields
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("title", "@price", "brand").build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics} smartphone")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(5)).scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams.of(new FTHybridCombineParams.Linear().alpha(0.5).beta(0.5)))
        .postProcessing(postProcessing).param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getResults().size(), greaterThan(0));

    // Field count and content assertions
    Map<String, Object> firstResult = reply.getResults().get(0);

    // LOAD specific fields should return: 3 loaded fields + score fields (4-5 total)
    assertThat(firstResult.size(), greaterThanOrEqualTo(4));
    assertThat(firstResult.size(), lessThanOrEqualTo(5));

    // Loaded fields should be present
    assertThat(firstResult, hasKey("title"));
    assertThat(firstResult, hasKey("price"));
    assertThat(firstResult, hasKey("brand"));

    // Non-loaded document fields should NOT be present
    assertThat(firstResult, not(hasKey("category")));
    assertThat(firstResult, not(hasKey("rating")));
    assertThat(firstResult, not(hasKey("image_embedding")));

    // Score fields may be present (text_score, vector_score) - this is expected
    // Document key (__key) should NOT be present when LOAD is used
    assertThat(firstResult, not(hasKey("__key")));
  }

  @Test
  // Since Redis 8.6.0 RC
  @SinceRedisVersion("8.5.0")
  public void testLoadAllFields() {
    // Test LOAD * to load all fields
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder().loadAll()
        .build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(3)).scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams.of(new FTHybridCombineParams.Linear().alpha(0.5).beta(0.5)))
        .postProcessing(postProcessing).param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getResults().size(), greaterThan(0));

    // Field count and content assertions
    Map<String, Object> firstResult = reply.getResults().get(0);

    // LOAD * should return: 5 document fields + 1 vector field + score fields (7-8 total)
    // This is MORE than LOAD specific fields (4-5)
    assertThat(firstResult.size(), greaterThanOrEqualTo(7));
    assertThat(firstResult.size(), lessThanOrEqualTo(8));

    // All document fields should be present
    assertThat(firstResult, hasKey("title"));
    assertThat(firstResult, hasKey("category"));
    assertThat(firstResult, hasKey("brand"));
    assertThat(firstResult, hasKey("price"));
    assertThat(firstResult, hasKey("rating"));

    // Vector field should also be present with LOAD *
    assertThat(firstResult, hasKey("image_embedding"));

    // Score fields should be present
    assertThat(firstResult, hasKey("text_score"));

    // Document key (__key) should NOT be present when LOAD is used
    assertThat(firstResult, not(hasKey("__key")));
  }

  @Test
  public void testLoadWithGroupBy() {
    // Test LOAD behavior with GROUPBY - loaded fields should be available for grouping
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("brand", "price", "category")
        .groupBy(FTHybridPostProcessingParams.GroupBy.of("brand")
            .reduce(FTHybridPostProcessingParams.Reducer
                .of(FTHybridPostProcessingParams.ReduceFunction.COUNT).as("count"))
            .reduce(FTHybridPostProcessingParams.Reducer
                .of(FTHybridPostProcessingParams.ReduceFunction.AVG, "@price").as("avg_price")))
        .build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams.of(new FTHybridCombineParams.Linear().alpha(0.5).beta(0.5)))
        .postProcessing(postProcessing).param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getResults().size(), greaterThan(0));

    // Field count and content assertions
    Map<String, Object> firstResult = reply.getResults().get(0);

    // After GROUPBY: only group key + reducers (3 fields total)
    assertThat(firstResult.size(), equalTo(3));

    // Grouping field should be present
    assertThat(firstResult, hasKey("brand"));

    // Reducer results should be present
    assertThat(firstResult, hasKey("count"));
    assertThat(firstResult, hasKey("avg_price"));

    // Original loaded fields (price, category) should NOT be present after GROUPBY
    // GROUPBY transforms the results, only group keys and reducers remain
    assertThat(firstResult, not(hasKey("price")));
    assertThat(firstResult, not(hasKey("category")));
    assertThat(firstResult, not(hasKey("title")));

    // Document key should NOT be present
    assertThat(firstResult, not(hasKey("__key")));
  }

  @Test
  public void testLoadWithApply() {
    // Test LOAD with APPLY - loaded fields should be available for expressions
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("price", "rating", "title")
        .apply(FTHybridPostProcessingParams.Apply.of("@price * @rating", "value_score"))
        .apply(FTHybridPostProcessingParams.Apply.of("@price * 0.9", "discounted"))
        .sortBy(
          FTHybridPostProcessingParams.SortBy.of(new FTHybridPostProcessingParams.SortProperty(
              "value_score", FTHybridPostProcessingParams.SortDirection.DESC)))
        .limit(FTHybridPostProcessingParams.Limit.of(0, 5)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("smartphone").scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams.of(new FTHybridCombineParams.Linear().alpha(0.5).beta(0.5)))
        .postProcessing(postProcessing).param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getResults().size(), greaterThan(0));

    // Field count and content assertions
    Map<String, Object> firstResult = reply.getResults().get(0);

    // LOAD with APPLY: 3 loaded fields + 2 computed fields + score fields (6-7 total)
    assertThat(firstResult.size(), greaterThanOrEqualTo(6));
    assertThat(firstResult.size(), lessThanOrEqualTo(7));

    // Loaded fields should be present
    assertThat(firstResult, hasKey("price"));
    assertThat(firstResult, hasKey("rating"));
    assertThat(firstResult, hasKey("title"));

    // Computed fields (from APPLY) should be present
    assertThat(firstResult, hasKey("value_score"));
    assertThat(firstResult, hasKey("discounted"));

    // Non-loaded document fields should NOT be present
    assertThat(firstResult, not(hasKey("category")));
    assertThat(firstResult, not(hasKey("brand")));
    assertThat(firstResult, not(hasKey("image_embedding")));

    // Document key should NOT be present when LOAD is used
    assertThat(firstResult, not(hasKey("__key")));
  }

  @Test
  public void testLoadWithFilter() {
    // Test LOAD with FILTER - loaded fields should be available for filtering
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("price", "brand", "title")
        .filter(FTHybridPostProcessingParams.Filter.of("@price > 700"))
        .sortBy(
          FTHybridPostProcessingParams.SortBy.of(new FTHybridPostProcessingParams.SortProperty(
              "price", FTHybridPostProcessingParams.SortDirection.ASC)))
        .build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams.of(new FTHybridCombineParams.Linear().alpha(0.5).beta(0.5)))
        .postProcessing(postProcessing).param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getResults().size(), greaterThan(0));

    // Verify loaded fields are present and FILTER condition is applied
    for (Map<String, Object> result : reply.getResults()) {
      // Field count: 3 loaded fields + score fields (4-5 total)
      assertThat(result.size(), greaterThanOrEqualTo(4));
      assertThat(result.size(), lessThanOrEqualTo(5));

      // Loaded fields should be present
      assertThat(result, hasKey("price"));
      assertThat(result, hasKey("brand"));
      assertThat(result, hasKey("title"));

      // FILTER condition: all results should have price > 700
      double price = Double.parseDouble((String) result.get("price"));
      assertThat(price, greaterThan(700.0));

      // Non-loaded document fields should NOT be present
      assertThat(result, not(hasKey("category")));
      assertThat(result, not(hasKey("rating")));
      assertThat(result, not(hasKey("image_embedding")));

      // Document key should NOT be present when LOAD is used
      assertThat(result, not(hasKey("__key")));
    }
  }

  @Test
  public void testLoadNoFields() {
    // Test without LOAD - should return only document IDs and scores
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .limit(FTHybridPostProcessingParams.Limit.of(0, 5)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("smartphone").scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(5)).scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams.of(new FTHybridCombineParams.Linear().alpha(0.5).beta(0.5)))
        .postProcessing(postProcessing).param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getResults(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getResults().size(), greaterThan(0));

    // Without LOAD, results should contain only keys and scores, NO document fields
    Map<String, Object> firstResult = reply.getResults().get(0);

    // NO LOAD: only __key + __score + score fields (3-4 total)
    // This is FEWER fields than LOAD specific (4-5) and LOAD * (7-8)
    assertThat(firstResult.size(), greaterThanOrEqualTo(3));
    assertThat(firstResult.size(), lessThanOrEqualTo(4));

    // Document key should be present
    assertThat(firstResult, hasKey("__key"));

    // Score fields should be present
    assertThat(firstResult, hasKey("__score"));

    // Document fields should NOT be present when no LOAD is specified
    assertThat(firstResult, not(hasKey("title")));
    assertThat(firstResult, not(hasKey("category")));
    assertThat(firstResult, not(hasKey("brand")));
    assertThat(firstResult, not(hasKey("price")));
    assertThat(firstResult, not(hasKey("rating")));
    assertThat(firstResult, not(hasKey("image_embedding")));
  }
}
