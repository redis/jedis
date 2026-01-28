package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeAll;
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

  public FTHybridCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testComprehensiveFtHybridWithAllFeatures() {
    // Clean up any existing index
    if (jedis.ftList().contains(INDEX_NAME)) {
      jedis.ftDropIndex(INDEX_NAME);
    }

    // Create index with text, tag, numeric, and vector fields
    Map<String, Object> vectorAttrs = new HashMap<>();
    vectorAttrs.put("TYPE", "FLOAT32");
    vectorAttrs.put("DIM", 10);
    vectorAttrs.put("DISTANCE_METRIC", "COSINE");

    assertOK(jedis.ftCreate(INDEX_NAME,
      FTCreateParams.createParams().on(IndexDataType.HASH).prefix(PREFIX), TextField.of("title"),
      TagField.of("category"), TagField.of("brand"), NumericField.of("price"),
      NumericField.of("rating"), VectorField.builder().fieldName("image_embedding")
          .algorithm(VectorField.VectorAlgorithm.HNSW).attributes(vectorAttrs).build()));

    // Add sample products - matching Lettuce test data
    createProductWithBrand("1", "Apple iPhone 15 Pro smartphone with advanced camera",
      "electronics", "apple", "999", "4.8",
      new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
    createProductWithBrand("2", "Samsung Galaxy S24 smartphone camera", "electronics", "samsung",
      "799", "4.6",
      new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f, 0.9f });
    createProductWithBrand("3", "Google Pixel 8 Pro camera smartphone", "electronics", "google",
      "699", "4.5", new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f });
    createProductWithBrand("4", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple",
      "999", "4.8", new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });
    createProductWithBrand("5", "Samsung Galaxy S24", "electronics", "samsung", "799", "4.6",
      new float[] { 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f, 0.9f });
    createProductWithBrand("6", "Google Pixel 8 Pro", "electronics", "google", "699", "4.5",
      new float[] { 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 0.8f });
    createProductWithBrand("7", "Best T-shirt", "apparel", "denim", "255", "4.2",
      new float[] { 0.12f, 0.22f, 0.32f, 0.42f, 0.52f, 0.62f, 0.72f, 0.82f, 0.92f, 0.85f });
    createProductWithBrand("8", "Best makeup", "beauty", "loreal", "155", "4.4",
      new float[] { 0.18f, 0.28f, 0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.88f, 0.98f, 0.75f });
    createProductWithBrand("9", "Best punching bag", "sports", "lonsdale", "733", "4.6",
      new float[] { 0.11f, 0.21f, 0.31f, 0.41f, 0.51f, 0.61f, 0.71f, 0.81f, 0.91f, 0.95f });
    createProductWithBrand("10", "Apple iPhone 15 Pro smartphone camera", "electronics", "apple",
      "999", "4.8", new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });

    // Execute comprehensive hybrid search with all features
    byte[] queryVector = floatArrayToByteArray(
      new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f });

    // Test @ prefix auto-addition: use fields without @ prefix
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("price", "brand", "@category") // Mix with and without @
        .groupBy(FTHybridPostProcessingParams.GroupBy.of("brand") // No @ prefix
            .reduce(FTHybridPostProcessingParams.Reducer
                .of(FTHybridPostProcessingParams.ReduceFunction.SUM, "@price").as("sum"))
            .reduce(FTHybridPostProcessingParams.Reducer
                .of(FTHybridPostProcessingParams.ReduceFunction.COUNT).as("count")))
        .apply(FTHybridPostProcessingParams.Apply.of("@sum * 0.9", "discounted_price"))
        .sortBy(FTHybridPostProcessingParams.SortBy.of(
          new FTHybridPostProcessingParams.SortProperty("sum",
              FTHybridPostProcessingParams.SortDirection.ASC), // No
          // @
          // prefix
          new FTHybridPostProcessingParams.SortProperty("count",
              FTHybridPostProcessingParams.SortDirection.DESC))) // No
        // @
        // prefix
        .filter(FTHybridPostProcessingParams.Filter.of("@sum > 700"))
        .limit(FTHybridPostProcessingParams.Limit.of(0, 20)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics} smartphone camera")
            .scorer(FTHybridSearchParams.Scorer.of("BM25")).scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(20).efRuntime(150))
            // Single combined filter expression
            .filter("(@brand:{apple|samsung|google}) (@price:[500 1500]) (@category:{electronics})")
            .scoreAlias("vector_score").build())
        .combine(FTHybridCombineParams
            .of(new FTHybridCombineParams.Linear().alpha(0.7).beta(0.3).window(25)))
        .postProcessing(postProcessing).param("discount_rate", "0.9").param("vector", queryVector)
        .build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    // Verify results - exact assertions like Lettuce
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

    // Clean up
    jedis.ftDropIndex(INDEX_NAME);
  }

  // Helper methods

  private void createProductWithBrand(String id, String title, String category, String brand,
      String price, String rating, float[] embedding) {
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
}
