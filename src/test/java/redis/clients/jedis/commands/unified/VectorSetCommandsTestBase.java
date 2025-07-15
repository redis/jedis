package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.VAddParams;

import java.util.List;

@Tag("integration")
@Tag("vector-set")
public abstract class VectorSetCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public VectorSetCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  private String POINTS_KEY;

  @BeforeEach
  public void setUp(TestInfo testInfo) {
    POINTS_KEY = testInfo.getDisplayName() + ":points";
    jedis.del(POINTS_KEY);
    // Add the example points from the Redis documentation
    // A: (1.0, 1.0), B: (-1.0, -1.0), C: (-1.0, 1.0), D: (1.0, -1.0), and E: (1.0, 0)
    jedis.vadd(POINTS_KEY, new float[]{1.0f, 1.0f}, "pt:A");
    jedis.vadd(POINTS_KEY, new float[]{-1.0f, -1.0f}, "pt:B");
    jedis.vadd(POINTS_KEY, new float[]{-1.0f, 1.0f}, "pt:C");
    jedis.vadd(POINTS_KEY, new float[]{1.0f, -1.0f}, "pt:D");
    jedis.vadd(POINTS_KEY, new float[]{1.0f, 0.0f}, "pt:E");
  }

  /**
   * Test the basic VADD method with float array.
   * Overload 1: vadd(String key, float[] vector, String element)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFloatArray(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:F";
    float[] vector = {1.0f, 2.0f};

    // Add a new element
    boolean result = jedis.vadd(testKey, vector, elementId);
    assertTrue(result);

    // Verify cardinality and dimension
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(2L, jedis.vdim(testKey));

    // Verify the vector was stored correctly
    List<Double> storedVector = jedis.vemb(testKey, elementId);
    assertEquals(2, storedVector.size());
    assertEquals(1.0, storedVector.get(0), 0.01);
    assertEquals(2.0, storedVector.get(1), 0.01);

    // Test duplicate addition - should return false
    result = jedis.vadd(testKey, vector, elementId);
    assertFalse(result);

    // Cardinality should remain the same
    assertEquals(1L, jedis.vcard(testKey));
  }

  /**
   * Test VADD method with float array and parameters.
   * Overload 2: vadd(String key, float[] vector, String element, VAddParams params)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFloatArrayAndParams(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:G";
    float[] vector = {1.0f, 2.0f};

    // Create parameters
    VAddParams params = new VAddParams();

    // Add a new element with parameters
    boolean result = jedis.vadd(testKey, vector, elementId, params);
    assertTrue(result);

    // Verify cardinality and dimension
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(2L, jedis.vdim(testKey));

    // Verify the vector was stored correctly
    List<Double> storedVector = jedis.vemb(testKey, elementId);
    assertEquals(2, storedVector.size());
    assertEquals(1.0, storedVector.get(0), 0.01);
    assertEquals(2.0, storedVector.get(1), 0.01);
  }

  /**
   * Test VADD method with FP32 byte blob.
   * Overload 3: vaddFP32(String key, byte[] vectorBlob, String element)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFP32ByteBlob(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:H";
    float[] vector = {1.0f, 2.0f};

    // Convert float array to FP32 byte blob
    byte[] vectorBlob = floatArrayToFP32Bytes(vector);

    // Add a new element with FP32 byte blob
    boolean result = jedis.vaddFP32(testKey, vectorBlob, elementId);
    assertTrue(result);

    // Verify cardinality and dimension
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(2L, jedis.vdim(testKey));

    // Verify the vector was stored correctly
    List<Double> storedVector = jedis.vemb(testKey, elementId);
    assertEquals(2, storedVector.size());
    assertEquals(1.0, storedVector.get(0), 0.01);
    assertEquals(2.0, storedVector.get(1), 0.01);
  }

  /**
   * Test VADD method with FP32 byte blob and parameters.
   * Overload 4: vaddFP32(String key, byte[] vectorBlob, String element, VAddParams params)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFP32ByteBlobAndParams(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:I";
    float[] vector = {1.0f, 2.0f};

    // Convert float array to FP32 byte blob
    byte[] vectorBlob = floatArrayToFP32Bytes(vector);

    // Create parameters
    VAddParams params = new VAddParams();

    // Add a new element with FP32 byte blob and parameters
    boolean result = jedis.vaddFP32(testKey, vectorBlob, elementId, params);
    assertTrue(result);

    // Verify cardinality and dimension
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(2L, jedis.vdim(testKey));

    // Verify the vector was stored correctly
    List<Double> storedVector = jedis.vemb(testKey, elementId);
    assertEquals(2, storedVector.size());
    assertEquals(1.0, storedVector.get(0), 0.01);
    assertEquals(2.0, storedVector.get(1), 0.01);
  }

  /**
   * Test VADD with quantization parameters.
   * Demonstrates how quantization parameters can be used with VADD.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithQuantization(TestInfo testInfo) {
    String baseKey = testInfo.getDisplayName() + ":test:vector:set";
    float[] vector = {1.0f, 2.0f};
    // Test with basic VADD first to establish a baseline
    String defaultKey = baseKey + ":default";
    jedis.del(defaultKey);
    boolean result = jedis.vadd(defaultKey, vector, "point:DEFAULT");
    assertTrue(result);

    List<Double> defaultVector = jedis.vemb(defaultKey, "point:DEFAULT");
    assertEquals(2, defaultVector.size());
    assertEquals(1.0, defaultVector.get(0), 0.01);
    assertEquals(2.0, defaultVector.get(1), 0.01);
    assertEquals(1L, jedis.vcard(defaultKey));

    // Test with Q8 quantization parameters
    String q8Key = baseKey + ":q8";
    VAddParams quantParams = new VAddParams().q8();
    jedis.del(q8Key);
    result = jedis.vadd(q8Key, vector, "point:Q8", quantParams);
    assertTrue(result);

    List<Double> quantVector = jedis.vemb(q8Key, "point:Q8");
    assertEquals(2, quantVector.size());
    assertEquals(1.0, quantVector.get(0), 0.01);
    assertEquals(2.0, quantVector.get(1), 0.01);
    assertEquals(1L, jedis.vcard(q8Key));

    // Test with NOQUANT quantization parameters
    String noQuantKey = baseKey + ":noQuant";
    VAddParams noQuantParams = new VAddParams().q8();
    jedis.del(noQuantKey);
    result = jedis.vadd(noQuantKey, vector, "point:NOQUANT", noQuantParams);
    assertTrue(result);

    List<Double> noQuantVector = jedis.vemb(noQuantKey, "point:NOQUANT");
    assertEquals(2, noQuantVector.size());
    assertEquals(1.0, noQuantVector.get(0), 0.01);
    assertEquals(2.0, noQuantVector.get(1), 0.01);
    assertEquals(1L, jedis.vcard(q8Key));
  }

  /**
   * Test VADD with dimension reduction using float array.
   * Verifies that high-dimensional vectors are reduced to target dimensions.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithReduceDimension(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:REDUCED";
    // Use a 4-dimensional vector that will be reduced to 2 dimensions
    float[] highDimVector = {1.0f, 2.0f, 3.0f, 4.0f};
    int targetDim = 2;

    // Create parameters for dimension reduction
    VAddParams params = new VAddParams();

    // Add element with dimension reduction
    boolean result = jedis.vadd(testKey, highDimVector, elementId, targetDim, params);
    assertTrue(result);

    // Verify cardinality
    assertEquals(1L, jedis.vcard(testKey));

    // Verify the vector was reduced to target dimensions
    assertEquals(targetDim, jedis.vdim(testKey));

    // Retrieve and verify the reduced vector
    List<Double> reducedVector = jedis.vemb(testKey, elementId);
    assertEquals(targetDim, reducedVector.size());

    // The values will be different due to random projection, but should exist
    assertNotNull(reducedVector.get(0));
    assertNotNull(reducedVector.get(1));
  }

  /**
   * Test vaddFP32 with dimension reduction using byte blob.
   * Verifies that FP32 format vectors are properly reduced.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddFP32WithReduceDimension(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:FP32_REDUCED";
    // Use a 4-dimensional vector that will be reduced to 2 dimensions
    float[] highDimVector = {1.0f, 2.0f, 3.0f, 4.0f};
    int targetDim = 2;

    // Convert to FP32 byte blob
    byte[] vectorBlob = floatArrayToFP32Bytes(highDimVector);

    // Create parameters for dimension reduction
    VAddParams params = new VAddParams();

    // Add element with dimension reduction using FP32 format
    boolean result = jedis.vaddFP32(testKey, vectorBlob, elementId, targetDim, params);
    assertTrue(result);

    // Verify cardinality
    assertEquals(1L, jedis.vcard(testKey));

    // Verify the vector was reduced to target dimensions
    assertEquals(targetDim, jedis.vdim(testKey));

    // Retrieve and verify the reduced vector
    List<Double> reducedVector = jedis.vemb(testKey, elementId);
    assertEquals(targetDim, reducedVector.size());

    // The values will be different due to random projection, but should exist
    assertNotNull(reducedVector.get(0));
    assertNotNull(reducedVector.get(1));
  }

  /**
   * Test VADD with dimension reduction and additional parameters.
   * Verifies that REDUCE works alongside other VAddParams.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithReduceDimensionAndParams(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:REDUCED_WITH_PARAMS";
    // Use a 6-dimensional vector that will be reduced to 3 dimensions
    float[] highDimVector = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
    int targetDim = 3;

    // Create parameters with quantization and dimension reduction
    VAddParams params = new VAddParams().q8().ef(100);

    // Add element with dimension reduction and additional parameters
    boolean result = jedis.vadd(testKey, highDimVector, elementId, targetDim, params);
    assertTrue(result);

    // Verify cardinality
    assertEquals(1L, jedis.vcard(testKey));

    // Verify the vector was reduced to target dimensions
    assertEquals(targetDim, jedis.vdim(testKey));

    // Retrieve and verify the reduced vector
    List<Double> reducedVector = jedis.vemb(testKey, elementId);
    assertEquals(targetDim, reducedVector.size());

    // All dimensions should have values (may be quantized)
    for (Double value : reducedVector) {
      assertNotNull(value);
    }
  }

  /**
   * Helper method to convert float array to FP32 byte blob (IEEE 754 format).
   */
  private byte[] floatArrayToFP32Bytes(float[] floats) {
    byte[] bytes = new byte[floats.length * 4]; // 4 bytes per float
    for (int i = 0; i < floats.length; i++) {
      int bits = Float.floatToIntBits(floats[i]);
      bytes[i * 4] = (byte) (bits & 0xFF);
      bytes[i * 4 + 1] = (byte) ((bits >> 8) & 0xFF);
      bytes[i * 4 + 2] = (byte) ((bits >> 16) & 0xFF);
      bytes[i * 4 + 3] = (byte) ((bits >> 24) & 0xFF);
    }
    return bytes;
  }
}
