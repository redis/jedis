package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.params.VSimParams;

/**
 * Integration tests for Redis Vector Sets based on the examples from the Redis documentation.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/">Redis Vector Sets
 *      Documentation</a>
 */
@Tag("integration")
@Tag("vector-set")
public abstract class ExtendedVectorSetCommandsTestBase extends UnifiedJedisCommandsTestBase {

  private String POINTS_KEY;

  public ExtendedVectorSetCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void setUp(TestInfo testInfo) {
    POINTS_KEY = testInfo.getDisplayName() + ":points";
    jedis.flushAll();

    // Add the example points from the Redis documentation
    // A: (1.0, 1.0), B: (-1.0, -1.0), C: (-1.0, 1.0), D: (1.0, -1.0), and E: (1.0, 0)
    jedis.vadd(POINTS_KEY, new float[] { 1.0f, 1.0f }, "pt:A");
    jedis.vadd(POINTS_KEY, new float[] { -1.0f, -1.0f }, "pt:B");
    jedis.vadd(POINTS_KEY, new float[] { -1.0f, 1.0f }, "pt:C");
    jedis.vadd(POINTS_KEY, new float[] { 1.0f, -1.0f }, "pt:D");
    jedis.vadd(POINTS_KEY, new float[] { 1.0f, 0.0f }, "pt:E");
  }

  /**
   * Test basic vector set operations as shown in the Redis documentation.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testBasicOperations() {

    jedis.type(POINTS_KEY);
    assertEquals("vectorset", jedis.type(POINTS_KEY));

    // Check the cardinality of the vector set
    long count = jedis.vcard(POINTS_KEY);
    assertEquals(5L, count);

    // Check the dimensionality of the vectors
    long dim = jedis.vdim(POINTS_KEY);
    assertEquals(2L, dim);

    // Retrieve the vectors for each point
    List<Double> vectorA = jedis.vemb(POINTS_KEY, "pt:A");
    assertEquals(2, vectorA.size());
    assertEquals(1.0, vectorA.get(0), 0.001);
    assertEquals(1.0, vectorA.get(1), 0.001);

    List<Double> vectorB = jedis.vemb(POINTS_KEY, "pt:B");
    assertEquals(2, vectorB.size());
    assertEquals(-1.0, vectorB.get(0), 0.001);
    assertEquals(-1.0, vectorB.get(1), 0.001);

    List<Double> vectorC = jedis.vemb(POINTS_KEY, "pt:C");
    assertEquals(2, vectorC.size());
    assertEquals(-1.0, vectorC.get(0), 0.001);
    assertEquals(1.0, vectorC.get(1), 0.001);

    List<Double> vectorD = jedis.vemb(POINTS_KEY, "pt:D");
    assertEquals(2, vectorD.size());
    assertEquals(1.0, vectorD.get(0), 0.001);
    assertEquals(-1.0, vectorD.get(1), 0.001);

    List<Double> vectorE = jedis.vemb(POINTS_KEY, "pt:E");
    assertEquals(2, vectorE.size());
    assertEquals(1.0, vectorE.get(0), 0.001);
    assertEquals(0.0, vectorE.get(1), 0.001);
  }

  /**
   * Test adding and removing elements from a vector set.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testAddAndRemoveElements() {
    // Add a new point F at (0, 0)
    boolean result = jedis.vadd(POINTS_KEY, new float[] { 0.0f, 0.0f }, "pt:F");
    assertTrue(result);

    // Check the updated cardinality
    long count = jedis.vcard(POINTS_KEY);
    assertEquals(6L, count);

    // Remove point F
    result = jedis.vrem(POINTS_KEY, "pt:F");
    assertTrue(result);

    // Check the cardinality after removal
    count = jedis.vcard(POINTS_KEY);
    assertEquals(5L, count);
  }

  /**
   * Test vector similarity search as shown in the Redis documentation.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVectorSimilaritySearch() {
    // Search for vectors similar to (0.9, 0.1)
    List<String> similar = jedis.vsim(POINTS_KEY, new float[] { 0.9f, 0.1f });
    assertNotNull(similar);
    assertFalse(similar.isEmpty());

    // The expected order based on similarity to (0.9, 0.1) should be:
    // E (1.0, 0.0), A (1.0, 1.0), D (1.0, -1.0), C (-1.0, 1.0), B (-1.0, -1.0)
    assertEquals("pt:E", similar.get(0));
    assertEquals("pt:A", similar.get(1));
    assertEquals("pt:D", similar.get(2));
    assertEquals("pt:C", similar.get(3));
    assertEquals("pt:B", similar.get(4));

    // Search for vectors similar to point A with scores
    VSimParams params = new VSimParams();
    Map<String, Double> similarWithScores = jedis.vsimByElementWithScores(POINTS_KEY, "pt:A",
      params);
    assertNotNull(similarWithScores);
    assertFalse(similarWithScores.isEmpty());

    // Point A should have a perfect similarity score of 1.0 with itself
    assertEquals(1.0, similarWithScores.get("pt:A"), 0.001);

    // Limit the number of results to 4
    params = new VSimParams().count(4);
    similar = jedis.vsimByElement(POINTS_KEY, "pt:A", params);
    assertEquals(4, similar.size());
  }

  /**
   * Test random sampling from a vector set.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testRandomSampling() {
    // Get a single random element
    String randomElement = jedis.vrandmember(POINTS_KEY);
    assertNotNull(randomElement);
    assertTrue(randomElement.startsWith("pt:"));

    // Get multiple random elements
    List<String> randomElements = jedis.vrandmember(POINTS_KEY, 3);
    assertEquals(3, randomElements.size());
    for (String element : randomElements) {
      assertTrue(element.startsWith("pt:"));
    }
  }

  /**
   * Test HNSW graph links.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testHnswGraphLinks() {
    // Get links for point A
    List<List<String>> links = jedis.vlinks(POINTS_KEY, "pt:A");
    assertNotNull(links);

    // Get links with scores
    List<Map<String, Double>> linksWithScores = jedis.vlinksWithScores(POINTS_KEY, "pt:A");
    assertNotNull(linksWithScores);
  }

  @Test
  @SinceRedisVersion("8.0.0")
  public void testAttributeOperations() {
    // Set attributes for point A
    String attributes = "{\"name\":\"Point A\",\"description\":\"First point added\"}";
    boolean result = jedis.vsetattr(POINTS_KEY, "pt:A", attributes);
    assertTrue(result);

    // Get attributes for point A
    String retrievedAttributes = jedis.vgetattr(POINTS_KEY, "pt:A");
    assertTrue(retrievedAttributes.contains("\"name\":\"Point A\""));
    assertTrue(retrievedAttributes.contains("\"description\":\"First point added\""));

    // Delete attributes by setting an empty string
    result = jedis.vsetattr(POINTS_KEY, "pt:A", "");
    assertTrue(result);

    // Verify attributes are deleted
    retrievedAttributes = jedis.vgetattr(POINTS_KEY, "pt:A");
    assertNull(retrievedAttributes);
  }

  @Test
  @SinceRedisVersion("8.0.0")
  public void testFilteredVectorSimilaritySearch() {
    // Set attributes for all points
    jedis.vsetattr(POINTS_KEY, "pt:A", "{\"size\":\"large\",\"price\":18.99}");
    jedis.vsetattr(POINTS_KEY, "pt:B", "{\"size\":\"large\",\"price\":35.99}");
    jedis.vsetattr(POINTS_KEY, "pt:C", "{\"size\":\"large\",\"price\":25.99}");
    jedis.vsetattr(POINTS_KEY, "pt:D", "{\"size\":\"small\",\"price\":21.00}");
    jedis.vsetattr(POINTS_KEY, "pt:E", "{\"size\":\"small\",\"price\":17.75}");

    // Filter by size = "large"
    VSimParams params = new VSimParams().filter(".size == \"large\"");
    List<String> similar = jedis.vsimByElement(POINTS_KEY, "pt:A", params);
    assertEquals(3, similar.size());
    assertEquals(Arrays.asList("pt:A", "pt:C", "pt:B"), similar);

    // Filter by size = "large" AND price > 20.00
    params = new VSimParams().filter(".size == \"large\" && .price > 20.00");
    similar = jedis.vsimByElement(POINTS_KEY, "pt:A", params);
    assertEquals(2, similar.size());
    assertEquals(Arrays.asList("pt:C", "pt:B"), similar);
  }

  @Test
  @SinceRedisVersion("8.0.0")
  public void testQuantizationTypes() {
    // Test Q8 quantization
    VAddParams q8Params = new VAddParams().q8();
    jedis.vadd("quantSetQ8", new float[] { 1.262185f, 1.958231f }, "quantElement", q8Params);
    List<Double> q8Vector = jedis.vemb("quantSetQ8", "quantElement");
    assertEquals(2, q8Vector.size());

    // Test NOQUANT (no quantization)
    VAddParams noQuantParams = new VAddParams().noQuant();
    jedis.vadd("quantSetNoQ", new float[] { 1.262185f, 1.958231f }, "quantElement", noQuantParams);
    List<Double> noQuantVector = jedis.vemb("quantSetNoQ", "quantElement");
    assertEquals(2, noQuantVector.size());
    assertEquals(1.262185, noQuantVector.get(0), 0.0001);
    assertEquals(1.958231, noQuantVector.get(1), 0.0001);

    // Test BIN (binary) quantization
    VAddParams binParams = new VAddParams().bin();
    jedis.vadd("quantSetBin", new float[] { 1.262185f, 1.958231f }, "quantElement", binParams);
    List<Double> binVector = jedis.vemb("quantSetBin", "quantElement");
    assertEquals(2, binVector.size());
    // Binary quantization will convert values to either 1 or -1
    assertTrue(binVector.get(0) == 1.0 || binVector.get(0) == -1.0);
    assertTrue(binVector.get(1) == 1.0 || binVector.get(1) == -1.0);
  }
}
