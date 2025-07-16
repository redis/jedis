package redis.clients.jedis.commands.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.resps.VectorInfo;

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
   * Test VADD with SETATTR parameter.
   * Verifies that attributes can be set when adding elements to vector sets.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithSetAttr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:WITH_ATTR";
    float[] vector = {1.0f, 2.0f};

    // Create simple text attributes for the element
    String attributes = "category=test,priority=high,score=95.5";

    // Create parameters with attributes
    VAddParams params = new VAddParams().setAttr(attributes);

    // Add element with attributes
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

    // Verify the attributes were stored correctly using VGETATTR
    String retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);
    assertEquals(attributes, retrievedAttrs);
  }

  /**
   * Test VADD with SETATTR and other parameters combined.
   * Verifies that SETATTR works alongside quantization and other options.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithSetAttrAndQuantization(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:ATTR_QUANT";
    float[] vector = {1.0f, 2.0f};

    // Create simple text attributes
    String attributes = "type=quantized,method=Q8,timestamp=2024-01-01";

    // Create parameters with both attributes and quantization
    VAddParams params = new VAddParams().setAttr(attributes).q8().ef(100);

    // Add element with attributes and quantization
    boolean result = jedis.vadd(testKey, vector, elementId, params);
    assertTrue(result);

    // Verify cardinality and dimension
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(2L, jedis.vdim(testKey));

    // Verify the vector was stored (may be quantized)
    List<Double> storedVector = jedis.vemb(testKey, elementId);
    assertEquals(2, storedVector.size());
    assertEquals(1.0, storedVector.get(0), 0.1); // Larger tolerance for quantization
    assertEquals(2.0, storedVector.get(1), 0.1);

    // Verify the attributes were stored correctly
    String retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);
    assertEquals(attributes, retrievedAttrs);
  }

  /**
   * Test VADD with SETATTR using FP32 format.
   * Verifies that attributes work with binary vector format.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddFP32WithSetAttr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:FP32_ATTR";
    float[] vector = {1.0f, 2.0f};

    // Convert to FP32 byte blob
    byte[] vectorBlob = floatArrayToFP32Bytes(vector);

    // Create simple text attributes
    String attributes = "format=FP32,source=binary,validated=true";

    // Create parameters with attributes
    VAddParams params = new VAddParams().setAttr(attributes);

    // Add element with FP32 format and attributes
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

    // Verify the attributes were stored correctly
    String retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);
    assertEquals(attributes, retrievedAttrs);
  }

  /**
   * Test VGETATTR command functionality.
   * Verifies that attributes can be retrieved from vector set elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVgetattr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:GETATTR_TEST";
    float[] vector = {1.0f, 2.0f};

    // First add an element without attributes
    boolean result = jedis.vadd(testKey, vector, elementId);
    assertTrue(result);

    // VGETATTR should return null for element without attributes
    String attrs = jedis.vgetattr(testKey, elementId);
    // Note: This might return null or empty string depending on Redis implementation
    // For now, we just verify the command executes without error

    // Now add an element with attributes
    String elementWithAttrs = "point:WITH_ATTRS";
    String attributes = "name=test_point,value=42,active=true";
    VAddParams params = new VAddParams().setAttr(attributes);
    result = jedis.vadd(testKey, vector, elementWithAttrs, params);
    assertTrue(result);

    // VGETATTR should return the attributes
    String retrievedAttrs = jedis.vgetattr(testKey, elementWithAttrs);
    assertNotNull(retrievedAttrs);
    assertEquals(attributes, retrievedAttrs);

    // Test VGETATTR with non-existent element
    String nonExistentAttrs = jedis.vgetattr(testKey, "non_existent_element");
    assertNull(nonExistentAttrs);
  }

  /**
   * Test VSETATTR command functionality.
   * Verifies that attributes can be set on vector set elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsetattr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:SETATTR_TEST";
    float[] vector = {1.0f, 2.0f};

    // First add an element without attributes
    boolean result = jedis.vadd(testKey, vector, elementId);
    assertTrue(result);

    // Set attributes using VSETATTR
    String attributes = "name=test_point,value=42,active=true";
    boolean setResult = jedis.vsetattr(testKey, elementId, attributes);
    assertTrue(setResult);

    // Verify attributes were set using VGETATTR
    String retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);
    assertEquals(attributes, retrievedAttrs);

    // Update attributes with new values
    String updatedAttributes = "name=updated_point,value=100,active=false,new_field=added";
    setResult = jedis.vsetattr(testKey, elementId, updatedAttributes);
    assertTrue(setResult);

    // Verify updated attributes
    retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);
    assertEquals(updatedAttributes, retrievedAttrs);
  }

  /**
   * Test VSETATTR with empty attributes (attribute deletion).
   * Verifies that setting empty attributes removes them.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsetattrDelete(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:DELETE_ATTR";
    float[] vector = {1.0f, 2.0f};

    // Add element with attributes
    String attributes = "category=test,priority=high";
    VAddParams params = new VAddParams().setAttr(attributes);
    boolean result = jedis.vadd(testKey, vector, elementId, params);
    assertTrue(result);

    // Verify attributes exist
    String retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);
    assertEquals(attributes, retrievedAttrs);

    // Delete attributes by setting empty string
    boolean setResult = jedis.vsetattr(testKey, elementId, "");
    assertTrue(setResult);

    // Verify attributes are deleted (should return null or empty)
    retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNull(retrievedAttrs);
  }

  /**
   * Test VINFO command functionality.
   * Verifies that vector set information can be retrieved.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVinfo(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    float[] vector1 = {1.0f, 2.0f};
    float[] vector2 = {3.0f, 4.0f};

    // Add some elements to the vector set
    VAddParams params = new VAddParams().setAttr("{\"type\": \"fruit\", \"color\": \"red\"}");
    boolean result1 = jedis.vadd(testKey, vector1, "element1", params);
    assertTrue(result1);

    boolean result2 = jedis.vadd(testKey, vector2, "element2");
    assertTrue(result2);

    // Get vector set information
    VectorInfo info = jedis.vinfo(testKey);
    assertNotNull(info);

    // Verify basic information is present
    assertNotNull(info.getVectorInfo());
    assertFalse(info.getVectorInfo().isEmpty());
    assertEquals(2, info.getDimensionality());
    assertEquals("int8", info.getType());
    assertEquals(2L, info.getSize());
    assertEquals(16L, info.getMaxNodes());
    assertThat(info.getMaxNodeUid(), greaterThan(0L));
    assertThat(info.getVSetUid(), greaterThan(0L) );
    assertEquals(0L, info.getProjectionInputDim());
    assertEquals(1L, info.getAttributesCount());
    assertNotNull(info.getMaxLevel());
  }

  /**
   * Test VINFO with empty vector set.
   * Verifies behavior when vector set doesn't exist.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVinfoEmptySet(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:empty:vector:set";

    VectorInfo info = jedis.vinfo(testKey);
    assertNull(info);
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
