package redis.clients.jedis.commands.unified.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.IndexDataType;
import redis.clients.jedis.search.Scorer;
import redis.clients.jedis.search.Scorers;
import redis.clients.jedis.search.aggr.Group;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.SortedField;
import redis.clients.jedis.search.Apply;
import redis.clients.jedis.search.Filter;
import redis.clients.jedis.search.Combiners;
import redis.clients.jedis.search.hybrid.FTHybridParams;
import redis.clients.jedis.search.hybrid.FTHybridPostProcessingParams;
import redis.clients.jedis.search.hybrid.FTHybridSearchParams;
import redis.clients.jedis.search.hybrid.FTHybridVectorParams;
import redis.clients.jedis.search.hybrid.HybridResult;
import redis.clients.jedis.search.Limit;
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
      FTCreateParams.createParams().on(IndexDataType.HASH).prefix(PREFIX), TextField.of("id"),
      TextField.of("title"), TagField.of("category"), TagField.of("brand"),
      NumericField.of("price"), NumericField.of("rating"),
      VectorField.builder().fieldName("image_embedding").algorithm(VectorField.VectorAlgorithm.HNSW)
          .attributes(vectorAttrs).build()));
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
    fields.put("id".getBytes(), id.getBytes());
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
        .groupBy(new Group("@brand").reduce(Reducers.sum("@price").as("sum"))
            .reduce(Reducers.count().as("count")))
        .apply(Apply.of("@sum * 0.9", "discounted_price"))
        .sortBy(SortedField.asc("@sum"), SortedField.desc("@count")).filter(Filter.of("@sum > 700"))
        .limit(Limit.of(0, 20)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics} smartphone camera")
            .scorer(Scorers.bm25std()).scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(20).efRuntime(150))
            .filter("(@brand:{apple|samsung|google}) (@price:[500 1500]) (@category:{electronics})")
            .scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.7).beta(0.3).window(25)).postProcessing(postProcessing)
        .param("discount_rate", "0.9").param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));
    assertThat(reply.getTotalResults(), equalTo(3L));
    assertThat(reply.getDocuments().size(), equalTo(3));
    assertThat(reply.getWarnings().size(), greaterThanOrEqualTo(0));
    assertThat(reply.getExecutionTime(), greaterThan(0.0));

    // Verify first result (google) - exact field values
    Document r1 = reply.getDocuments().get(0);
    assertThat(r1.get("brand"), equalTo("google"));
    assertThat(r1.get("count"), equalTo("2"));
    assertThat(r1.get("sum"), equalTo("1398"));
    assertThat(r1.get("discounted_price"), equalTo("1258.2"));

    // Verify second result (samsung) - exact field values
    Document r2 = reply.getDocuments().get(1);
    assertThat(r2.get("brand"), equalTo("samsung"));
    assertThat(r2.get("count"), equalTo("2"));
    assertThat(r2.get("sum"), equalTo("1598"));
    assertThat(r2.get("discounted_price"), equalTo("1438.2"));

    // Verify third result (apple) - exact field values
    Document r3 = reply.getDocuments().get(2);
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
        .combine(Combiners.linear().alpha(0.5).beta(0.5)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Field count and content assertions
    Document firstResult = reply.getDocuments().get(0);

    // Loaded fields should be present
    assertThat(firstResult.hasProperty("title"), equalTo(true));
    assertThat(firstResult.hasProperty("price"), equalTo(true));
    assertThat(firstResult.hasProperty("brand"), equalTo(true));

    // Non-loaded document fields should NOT be present
    assertThat(firstResult.hasProperty("category"), equalTo(false));
    assertThat(firstResult.hasProperty("rating"), equalTo(false));
    assertThat(firstResult.hasProperty("image_embedding"), equalTo(false));
  }

  @Test
  @SinceRedisVersion("8.6.0")
  public void testLoadAllFields() {
    // Test LOAD * to load all fields
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder().loadAll()
        .build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(3)).scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.5).beta(0.5)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Field count and content assertions
    Document firstResult = reply.getDocuments().get(0);

    // All document fields should be present
    assertThat(firstResult.hasProperty("title"), equalTo(true));
    assertThat(firstResult.hasProperty("category"), equalTo(true));
    assertThat(firstResult.hasProperty("brand"), equalTo(true));
    assertThat(firstResult.hasProperty("price"), equalTo(true));
    assertThat(firstResult.hasProperty("rating"), equalTo(true));

    // Vector field should also be present with LOAD *
    assertThat(firstResult.hasProperty("image_embedding"), equalTo(true));

    // Score fields should be present
    assertThat(firstResult.hasProperty("text_score"), equalTo(true));
  }

  @Test
  public void testLoadWithGroupBy() {
    // Test LOAD behavior with GROUPBY - loaded fields should be available for grouping
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("brand", "price", "category").groupBy(new Group("@brand")
            .reduce(Reducers.count().as("count")).reduce(Reducers.avg("@price").as("avg_price")))
        .build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.5).beta(0.5)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Field count and content assertions
    Document firstResult = reply.getDocuments().get(0);

    // Grouping field should be present
    assertThat(firstResult.hasProperty("brand"), equalTo(true));

    // Reducer results should be present
    assertThat(firstResult.hasProperty("count"), equalTo(true));
    assertThat(firstResult.hasProperty("avg_price"), equalTo(true));

    // Original loaded fields (price, category) should NOT be present after GROUPBY
    // GROUPBY transforms the results, only group keys and reducers remain
    assertThat(firstResult.hasProperty("price"), equalTo(false));
    assertThat(firstResult.hasProperty("category"), equalTo(false));
    assertThat(firstResult.hasProperty("title"), equalTo(false));
  }

  @Test
  public void testLoadWithApply() {
    // Test LOAD with APPLY - loaded fields should be available for expressions
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("price", "rating", "title").apply(Apply.of("@price * @rating", "value_score"))
        .apply(Apply.of("@price * 0.9", "discounted")).sortBy(SortedField.desc("@value_score"))
        .limit(Limit.of(0, 5)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("smartphone").scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.5).beta(0.5).window(25)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Field count and content assertions
    Document firstResult = reply.getDocuments().get(0);

    // Loaded fields should be present
    assertThat(firstResult.hasProperty("price"), equalTo(true));
    assertThat(firstResult.hasProperty("rating"), equalTo(true));
    assertThat(firstResult.hasProperty("title"), equalTo(true));

    // Computed fields (from APPLY) should be present
    assertThat(firstResult.hasProperty("value_score"), equalTo(true));
    assertThat(firstResult.hasProperty("discounted"), equalTo(true));

    // Non-loaded document fields should NOT be present
    assertThat(firstResult.hasProperty("category"), equalTo(false));
    assertThat(firstResult.hasProperty("brand"), equalTo(false));
    assertThat(firstResult.hasProperty("image_embedding"), equalTo(false));
  }

  @Test
  public void testLoadWithFilter() {
    // Test LOAD with FILTER - loaded fields should be available for filtering
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("price", "brand", "title").filter(Filter.of("@price > 700"))
        .sortBy(SortedField.asc("@price")).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.5).beta(0.5)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Verify loaded fields are present and FILTER condition is applied
    for (Document result : reply.getDocuments()) {
      // Loaded fields should be present
      assertThat(result.hasProperty("price"), equalTo(true));
      assertThat(result.hasProperty("brand"), equalTo(true));
      assertThat(result.hasProperty("title"), equalTo(true));

      // FILTER condition: all results should have price > 700
      double price = Double.parseDouble(result.getString("price"));
      assertThat(price, greaterThan(700.0));

      // Non-loaded document fields should NOT be present
      assertThat(result.hasProperty("category"), equalTo(false));
      assertThat(result.hasProperty("rating"), equalTo(false));
      assertThat(result.hasProperty("image_embedding"), equalTo(false));
    }
  }

  @Test
  public void testLoadNoFields() {
    // Test without LOAD - should return only document IDs and scores
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .limit(Limit.of(0, 5)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("smartphone").scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(5)).scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.5).beta(0.5)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Without LOAD, results should contain only keys and scores, NO document fields
    Document firstResult = reply.getDocuments().get(0);

    // Document id and score should be present (extracted from __key and __score)
    assertThat(firstResult.getId(), notNullValue());
    assertThat(firstResult.getScore(), notNullValue());

    // Document fields should NOT be present when no LOAD is specified
    assertThat(firstResult.hasProperty("title"), equalTo(false));
    assertThat(firstResult.hasProperty("category"), equalTo(false));
    assertThat(firstResult.hasProperty("brand"), equalTo(false));
    assertThat(firstResult.hasProperty("price"), equalTo(false));
    assertThat(firstResult.hasProperty("rating"), equalTo(false));
    assertThat(firstResult.hasProperty("image_embedding"), equalTo(false));
  }

  @Test
  public void testNoSort() {
    // Test NOSORT - disables default sorting by score
    FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder()
        .load("title", "price").noSort().limit(Limit.of(0, 10)).build();

    FTHybridParams hybridArgs = FTHybridParams.builder()
        .search(FTHybridSearchParams.builder().query("@category:{electronics}")
            .scoreAlias("text_score").build())
        .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
            .method(FTHybridVectorParams.Knn.of(10)).scoreAlias("vector_score").build())
        .combine(Combiners.linear().alpha(0.5).beta(0.5)).postProcessing(postProcessing)
        .param("vector", queryVector).build();

    HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

    assertThat(reply, notNullValue());
    assertThat(reply.getDocuments(), not(empty()));

    // Result count assertions
    assertThat(reply.getTotalResults(), greaterThan(0L));
    assertThat(reply.getDocuments().size(), greaterThan(0));

    // Loaded fields should be present
    Document firstResult = reply.getDocuments().get(0);
    assertThat(firstResult.hasProperty("title"), equalTo(true));
    assertThat(firstResult.hasProperty("price"), equalTo(true));
  }

  // ========== Scorer Tests ==========

  /**
   * Nested test class to verify all supported scorers work correctly with FT.HYBRID command. Tests
   * each scorer from {@link Scorers} to ensure proper integration.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class SupportedScorersTest {

    /**
     * Provides scorer instances and their expected scores for parameterized testing. Sccore values
     * might differ between cluster and standalone modes. To perform basic verification use same
     * values for both cluster/standalone with tolerance.
     * @return Stream of Arguments containing (Scorer, expectedScore, tolerance)
     */
    Stream<Arguments> scorerProvider() {
      return Stream.of(Arguments.of(Scorers.tfidf(), 2.5, 0.5),
        Arguments.of(Scorers.tfidfDocnorm(), 0.2, 0.5), Arguments.of(Scorers.bm25stdNorm(), 1, 0.5),
        Arguments.of(Scorers.bm25std(), 1.3, 0.5), Arguments.of(Scorers.dismax(), 1.0, 0.5),
        Arguments.of(Scorers.docscore(), 1.0, 0.5), Arguments.of(Scorers.hamming(), 0.0, 0.5));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scorerProvider")
    public void testScorer(Scorer scorer, double expectedScore, double tolerance) {
      // Create hybrid search with the provided scorer
      FTHybridParams hybridArgs = FTHybridParams.builder()
          .search(FTHybridSearchParams.builder().query("@id:1").scorer(scorer)
              .scoreAlias("text_score").build())
          .vectorSearch(FTHybridVectorParams.builder().field("@image_embedding").vector("vector")
              .filter("@id:1").method(FTHybridVectorParams.Knn.of(5)).scoreAlias("vector_score")
              .build())
          .param("vector", queryVector).build();

      // Execute hybrid search
      HybridResult result = jedis.ftHybrid(INDEX_NAME, hybridArgs);

      // Verify results are returned
      assertThat(result, notNullValue());
      assertThat(result.getTotalResults(), equalTo(1L));

      // Verify scorer is working - text_score should be present
      Document firstDoc = result.getDocuments().get(0);
      assertThat(firstDoc.hasProperty("text_score"), equalTo(true));

      // Verify score is valid and within expected range
      double scoreValue = Double.parseDouble(firstDoc.getString("text_score"));
      assertThat(scoreValue, closeTo(expectedScore, tolerance));
    }
  }

  // ========== HybridResult Population Tests ==========

  /**
   * Verify that HybridResult is properly populated from FT.HYBRID command responses. Tests all
   * HybridResult fields and Document structure in various scenarios.
   */
  @Nested
  @Tag("integration")
  class HybridResultPopulationTest {

    /**
     * Verify : This command will only return document IDs (keyid) and scores to which the user has
     * read access. To retrieve entire documents, use projections with LOAD * or LOAD <count> field
     */
    @Test
    public void verifyHybridResultBasicFieldsNoLoad() {
      FTHybridParams hybridArgs = FTHybridParams.builder()
          .search(FTHybridSearchParams.builder().query("@id:1").build())
          .vectorSearch(FTHybridVectorParams.builder().filter("@id:1").field("@image_embedding")
              .vector("vector").method(FTHybridVectorParams.Knn.of(5)).build())
          .param("vector", queryVector).build();

      HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

      // Verify HybridResult is not null
      assertThat(reply, notNullValue());

      // Verify totalResults is populated and > 0
      assertThat(reply.getTotalResults(), equalTo(1L));

      // Verify executionTime is populated and reasonable
      assertThat(reply.getExecutionTime(), greaterThan(0.0));

      // Verify documents list is populated
      assertThat(reply.getDocuments(), notNullValue());
      assertThat(reply.getDocuments(), not(empty()));

      Document doc = reply.getDocuments().get(0);
      assertThat(doc.getId(), equalTo("product:hybrid:1"));
      assertThat(doc.hasProperty("title"), equalTo(false));
      assertThat(doc.getScore(), closeTo(0.03, 0.01));

      // Verify warnings list is not null (may be empty)
      assertThat(reply.getWarnings(), notNullValue());
      assertThat(reply.getWarnings(), empty());
    }

    /**
     * Verify : This command will only return document IDs (keyid) and scores to which the user has
     * read access. To retrieve entire documents, use projections with LOAD * or LOAD <count> field
     */
    @Test
    public void verifyHybridResultBasicFieldsWithLoadAll() {
      // Execute a simple hybrid search with known results
      FTHybridPostProcessingParams postProcessing = FTHybridPostProcessingParams.builder().loadAll()
          .build();

      FTHybridParams hybridArgs = FTHybridParams.builder()
          .search(FTHybridSearchParams.builder().query("@id:1").build())
          .vectorSearch(FTHybridVectorParams.builder().filter("@id:1").field("@image_embedding")
              .vector("vector").method(FTHybridVectorParams.Knn.of(5)).build())
          .combine(Combiners.linear().alpha(1).beta(0)).postProcessing(postProcessing)
          .param("vector", queryVector).build();

      HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

      // Verify HybridResult is not null
      assertThat(reply, notNullValue());

      // Verify totalResults is populated and > 0
      assertThat(reply.getTotalResults(), equalTo(1L));

      // Verify executionTime is populated and reasonable
      assertThat(reply.getExecutionTime(), greaterThan(0.0));

      // Verify documents list is populated
      assertThat(reply.getDocuments(), notNullValue());
      assertThat(reply.getDocuments(), not(empty()));

      Document doc = reply.getDocuments().get(0);
      assertThat(doc.getId(), nullValue());
      assertThat(doc.hasProperty("title"), equalTo(true));
      assertThat(doc.get("title"), equalTo("Apple iPhone 15 Pro smartphone with advanced camera"));
      assertThat(doc.getScore(), closeTo(0.5, 0.5));

      // Verify warnings list is not null (may be empty)
      assertThat(reply.getWarnings(), notNullValue());
      assertThat(reply.getWarnings(), empty());
    }

    @Test
    public void verifyHybridResultWithEmptyResults() {
      // Execute hybrid search with filter that matches no documents
      FTHybridParams hybridArgs = FTHybridParams.builder()
          .search(FTHybridSearchParams.builder().query("@category:{nonexistent}").build())
          .vectorSearch(
            FTHybridVectorParams.builder().filter("@id:nonexistent").field("@image_embedding")
                .vector("vector").method(FTHybridVectorParams.Knn.of(5)).build())
          .param("vector", queryVector).build();

      HybridResult reply = jedis.ftHybrid(INDEX_NAME, hybridArgs);

      // Verify HybridResult is not null even with empty results
      assertThat(reply, notNullValue());

      // Verify totalResults is 0
      assertThat(reply.getTotalResults(), equalTo(0L));

      // Verify documents list is empty
      assertThat(reply.getDocuments(), notNullValue());
      assertThat(reply.getDocuments(), empty());

      // Verify executionTime is still populated (>= 0.0)
      assertThat(reply.getExecutionTime(), greaterThanOrEqualTo(0.0));
    }
  }
}
