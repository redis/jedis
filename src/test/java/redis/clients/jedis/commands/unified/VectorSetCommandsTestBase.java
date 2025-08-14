package redis.clients.jedis.commands.unified;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.util.VectorTestUtils.floatArrayToFP32Bytes;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.params.VSimParams;
import redis.clients.jedis.resps.RawVector;
import redis.clients.jedis.resps.VectorInfo;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.VectorTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@Tag("integration")
@Tag("vector-set")
public abstract class VectorSetCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public VectorSetCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  /**
   * Test the basic VADD method with float array. Overload 1: vadd(String key, float[] vector,
   * String element)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFloatArray(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:F";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VADD method with float array and parameters. Overload 2: vadd(String key, float[] vector,
   * String element, VAddParams params)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFloatArrayAndParams(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:G";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VADD method with FP32 byte blob. Overload 3: vaddFP32(String key, byte[] vectorBlob,
   * String element)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFP32ByteBlob(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:H";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VADD method with FP32 byte blob and parameters. Overload 4: vaddFP32(String key, byte[]
   * vectorBlob, String element, VAddParams params)
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithFP32ByteBlobAndParams(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:I";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VADD with quantization parameters. Demonstrates how quantization parameters can be used
   * with VADD.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithQuantization(TestInfo testInfo) {
    String baseKey = testInfo.getDisplayName() + ":test:vector:set";
    float[] vector = { 1.0f, 2.0f };
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
    assertEquals(1L, jedis.vcard(noQuantKey));
  }

  /**
   * Test VADD with dimension reduction using float array. Verifies that high-dimensional vectors
   * are reduced to target dimensions.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithReduceDimension(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:REDUCED";
    // Use a 4-dimensional vector that will be reduced to 2 dimensions
    float[] highDimVector = { 1.0f, 2.0f, 3.0f, 4.0f };
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
   * Test vaddFP32 with dimension reduction using byte blob. Verifies that FP32 format vectors are
   * properly reduced.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddFP32WithReduceDimension(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:FP32_REDUCED";
    // Use a 4-dimensional vector that will be reduced to 2 dimensions
    float[] highDimVector = { 1.0f, 2.0f, 3.0f, 4.0f };
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
   * Test VADD with dimension reduction and additional parameters. Verifies that REDUCE works
   * alongside other VAddParams.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithReduceDimensionAndParams(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:REDUCED_WITH_PARAMS";
    // Use a 6-dimensional vector that will be reduced to 3 dimensions
    float[] highDimVector = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
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
   * Test VADD with SETATTR parameter. Verifies that attributes can be set when adding elements to
   * vector sets.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithSetAttr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:WITH_ATTR";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VADD with SETATTR and other parameters combined. Verifies that SETATTR works alongside
   * quantization and other options.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddWithSetAttrAndQuantization(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:ATTR_QUANT";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VADD with SETATTR using FP32 format. Verifies that attributes work with binary vector
   * format.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVaddFP32WithSetAttr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:FP32_ATTR";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VGETATTR command functionality. Verifies that attributes can be retrieved from vector set
   * elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVgetattr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:GETATTR_TEST";
    float[] vector = { 1.0f, 2.0f };

    // First add an element without attributes
    boolean result = jedis.vadd(testKey, vector, elementId);
    assertTrue(result);

    // VGETATTR should return null for element without attributes
    String attrs = jedis.vgetattr(testKey, elementId);
    assertNull(attrs);

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
   * Test VGETATTR with binary key and element. Verifies that VGETATTR works with byte array keys
   * and elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVgetattrBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();
    byte[] elementId = "binary_element_with_attrs".getBytes();
    float[] vector = { 1.0f, 2.0f };

    // VGETATTR should return null for element without attributes
    assertNull(jedis.vgetattr(testKey, elementId));

    // Now add an element with attributes using binary key and element
    String attributes = "name=binary_test_point,value=42,active=true";
    VAddParams params = new VAddParams().setAttr(attributes);
    boolean result = jedis.vadd(testKey, vector, elementId, params);
    assertTrue(result);

    // VGETATTR should return the attributes as byte array
    byte[] retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);

    // Convert byte array back to string and verify content
    String retrievedAttrsString = SafeEncoder.encode(retrievedAttrs);
    assertEquals(attributes, retrievedAttrsString);
  }

  /**
   * Test VSETATTR command functionality. Verifies that attributes can be set on vector set
   * elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsetattr(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:SETATTR_TEST";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VSETATTR with binary key and element. Verifies that VSETATTR works with byte array keys
   * and elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsetattrBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();
    byte[] elementId = "binary_setattr_element".getBytes();
    float[] vector = { 1.0f, 2.0f };

    // First add an element without attributes
    boolean result = jedis.vadd(testKey, vector, elementId);
    assertTrue(result);

    // Set attributes using binary VSETATTR
    String attributes = "name=binary_test_point,value=42,active=true";
    byte[] attributesBytes = attributes.getBytes();
    boolean setResult = jedis.vsetattr(testKey, elementId, attributesBytes);
    assertTrue(setResult);

    // Verify attributes were set using binary VGETATTR
    byte[] retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);

    // Convert back to string and verify
    String retrievedAttrsString = SafeEncoder.encode(retrievedAttrs);
    assertEquals(attributes, retrievedAttrsString);

    // Update attributes with new values using binary VSETATTR
    String updatedAttributes = "name=updated_binary_point,value=100,active=false,new_field=added";
    byte[] updatedAttributesBytes = updatedAttributes.getBytes();
    setResult = jedis.vsetattr(testKey, elementId, updatedAttributesBytes);
    assertTrue(setResult);

    // Verify updated attributes using binary VGETATTR
    retrievedAttrs = jedis.vgetattr(testKey, elementId);
    assertNotNull(retrievedAttrs);

    String updatedRetrievedString = SafeEncoder.encode(retrievedAttrs);
    assertEquals(updatedAttributes, updatedRetrievedString);
  }

  /**
   * Test VLINKS command functionality. Verifies that vector set links can be retrieved correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVlinks(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    // Add some vectors to create a vector set with links
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };
    float[] vector3 = { 1.0f, 1.0f };

    jedis.vadd(testKey, vector1, "element1");
    jedis.vadd(testKey, vector2, "element2");
    jedis.vadd(testKey, vector3, "element3");

    // Get links for element1
    List<List<String>> links = jedis.vlinks(testKey, "element1");
    assertNotNull(links);

    assertFalse(links.isEmpty());
    for (List<String> linkList : links) {
      for (String rawLink : linkList) {
        assertTrue(rawLink.equals("element2") || rawLink.equals("element3"));
      }
    }
  }

  /**
   * Test VLINKS command functionality. Verifies that vector set links can be retrieved correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVlinksWithScores(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    // Add some vectors to create a vector set with links
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };
    float[] vector3 = { 1.0f, 1.0f };

    jedis.vadd(testKey, vector1, "element1");
    jedis.vadd(testKey, vector2, "element2");
    jedis.vadd(testKey, vector3, "element3");

    // Get links for element1
    List<Map<String, Double>> links = jedis.vlinksWithScores(testKey, "element1");
    assertNotNull(links);

    assertFalse(links.isEmpty());
    for (Map<String, Double> scores : links) {
      for (String element : scores.keySet()) {
        assertTrue(element.equals("element2") || element.equals("element3"));
        assertTrue(scores.get(element) > 0.0);
      }
    }
  }

  /**
   * Test VLINKS with binary key and element. Verifies that VLINKS works with byte array keys and
   * elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVlinksBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();
    byte[] elementId = "binary_element".getBytes();

    // Add vectors using binary key and elements
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };

    jedis.vadd(testKey, vector1, elementId);
    jedis.vadd(testKey, vector2, "element2".getBytes());

    // Get links using binary VLINKS
    List<List<byte[]>> binaryLinks = jedis.vlinks(testKey, elementId);
    assertNotNull(binaryLinks);
    assertThat(binaryLinks.size(), is(greaterThan(0)));

    // If there are links, verify they are valid strings
    for (List<byte[]> linkList : binaryLinks) {
      for (byte[] rawLink : linkList) {
        String link = SafeEncoder.encode(rawLink);
        assertThat(link, is(notNullValue()));
        assertThat(link, not(emptyString()));
      }
    }
  }

  /**
   * Test VLINKS command functionality. Verifies that vector set links can be retrieved correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVlinksBinaryWithScores(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();

    // Add some vectors to create a vector set with links
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };
    float[] vector3 = { 1.0f, 1.0f };

    jedis.vadd(testKey, vector1, "element1".getBytes());
    jedis.vadd(testKey, vector2, "element2".getBytes());
    jedis.vadd(testKey, vector3, "element3".getBytes());

    // Get links for element1
    List<Map<byte[], Double>> links = jedis.vlinksWithScores(testKey, "element1".getBytes());
    assertNotNull(links);

    assertFalse(links.isEmpty());
    for (Map<byte[], Double> scores : links) {
      for (byte[] element : scores.keySet()) {
        assertTrue(Arrays.equals(element, "element2".getBytes())
            || Arrays.equals(element, "element3".getBytes()));
        assertTrue(scores.get(element) > 0.0);
      }
    }
  }

  /**
   * Test VLINKS with non-existent element. Verifies that VLINKS handles non-existent elements
   * correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVlinksNonExistent(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:nonexistent";

    // Add a vector first
    float[] vector = { 1.0f, 2.0f };
    jedis.vadd(testKey, vector, "existing_element");

    // Try to get links for non-existent element
    List<List<String>> links = jedis.vlinks(testKey, "non_existent_element");
    // Should return empty list or null for non-existent elements
    // Exact behavior depends on Redis implementation
    assertTrue(links == null || links.isEmpty());
  }

  /**
   * Test VRANDMEMBER command functionality. Verifies that random vector set members can be
   * retrieved correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrandmember(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    // Add some vectors to the set
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };
    float[] vector3 = { 1.0f, 1.0f };

    jedis.vadd(testKey, vector1, "element1");
    jedis.vadd(testKey, vector2, "element2");
    jedis.vadd(testKey, vector3, "element3");

    // Get a single random member
    String randomMember = jedis.vrandmember(testKey);
    assertNotNull(randomMember);

    // Should be one of the added elements
    assertTrue(randomMember.equals("element1") || randomMember.equals("element2")
        || randomMember.equals("element3"));
  }

  /**
   * Test VRANDMEMBER with count parameter. Verifies that multiple random members can be retrieved.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrandmemberWithCount(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:count";

    // Add multiple vectors
    for (int i = 1; i <= 5; i++) {
      float[] vector = { (float) i, (float) i };
      jedis.vadd(testKey, vector, "element" + i);
    }

    // Get 3 random members
    List<String> randomMembers = jedis.vrandmember(testKey, 3);
    assertNotNull(randomMembers);
    assertEquals(3, randomMembers.size());

    // All returned members should be valid element IDs
    String[] validElements = { "element1", "element2", "element3", "element4", "element5" };
    for (String member : randomMembers) {
      assertTrue(asList(validElements).contains(member));
    }

    // Test with count larger than set size
    List<String> allMembers = jedis.vrandmember(testKey, 10);
    assertNotNull(allMembers);
    assertTrue(allMembers.size() <= 5); // Should not exceed actual set size
  }

  /**
   * Test VRANDMEMBER with binary key. Verifies that VRANDMEMBER works with byte array keys.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrandmemberBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();

    // Add vectors using binary key
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };

    jedis.vadd(testKey, vector1, "binary_element1".getBytes());
    jedis.vadd(testKey, vector2, "binary_element2".getBytes());

    // Get random member using binary key
    byte[] randomMember = jedis.vrandmember(testKey);
    assertNotNull(randomMember);

    // Convert to string for comparison
    String randomMemberStr = SafeEncoder.encode(randomMember);
    assertTrue(
      randomMemberStr.equals("binary_element1") || randomMemberStr.equals("binary_element2"));

    // Test with count using binary key
    List<byte[]> randomMembers = jedis.vrandmember(testKey, 2);
    assertNotNull(randomMembers);
    assertTrue(randomMembers.size() <= 2);

    // Verify all returned members are valid
    for (byte[] member : randomMembers) {
      assertNotNull(member);
      String memberStr = SafeEncoder.encode(member);
      assertTrue(memberStr.equals("binary_element1") || memberStr.equals("binary_element2"));
    }
  }

  /**
   * Test VRANDMEMBER with empty vector set. Verifies that VRANDMEMBER handles empty sets correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrandmemberEmptySet(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:empty:vector:set";

    // Try to get random member from empty/non-existent set
    String randomMember = jedis.vrandmember(testKey);
    // Should return null for empty set
    assertNull(randomMember);

    // Test with count on empty set
    List<String> randomMembers = jedis.vrandmember(testKey, 5);
    // Should return empty list for empty set
    assertTrue(randomMembers.isEmpty());
  }

  /**
   * Test VRANDMEMBER with single element. Verifies that VRANDMEMBER works correctly with only one
   * element.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrandmemberSingleElement(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:single";

    // Add only one vector
    float[] vector = { 1.0f, 2.0f };
    jedis.vadd(testKey, vector, "single_element");

    // Get random member (should always be the single element)
    String randomMember = jedis.vrandmember(testKey);
    assertNotNull(randomMember);
    assertEquals("single_element", randomMember);

    // Test with count
    List<String> randomMembers = jedis.vrandmember(testKey, 3);
    assertNotNull(randomMembers);
    assertEquals(1, randomMembers.size()); // Should only return the single element
    assertEquals("single_element", randomMembers.get(0));
  }

  /**
   * Test VRANDMEMBER with negative count. Verifies that VRANDMEMBER handles negative count (allows
   * duplicates).
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrandmemberNegativeCount(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:negative";

    // Add some vectors
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };

    jedis.vadd(testKey, vector1, "element1");
    jedis.vadd(testKey, vector2, "element2");

    // Get random members with negative count (allows duplicates)
    List<String> randomMembers = jedis.vrandmember(testKey, -5);
    assertNotNull(randomMembers);
    assertEquals(5, randomMembers.size()); // Should return exactly 5 elements (with possible
                                           // duplicates)

    // All returned members should be valid element IDs
    for (String member : randomMembers) {
      assertTrue(member.equals("element1") || member.equals("element2"));
    }
  }

  /**
   * Test VREM command functionality. Verifies that vector set elements can be removed correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVrem(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    // Add some vectors to the set
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };
    float[] vector3 = { 1.0f, 1.0f };

    jedis.vadd(testKey, vector1, "element1");
    jedis.vadd(testKey, vector2, "element2");
    jedis.vadd(testKey, vector3, "element3");

    // Verify initial cardinality
    assertEquals(3L, jedis.vcard(testKey));

    // Remove one element
    boolean removed = jedis.vrem(testKey, "element2");
    assertTrue(removed);
    assertEquals(2L, jedis.vcard(testKey));

    // Try to remove the same element again (should return false)
    removed = jedis.vrem(testKey, "element2");
    assertFalse(removed);
    assertEquals(2L, jedis.vcard(testKey));

    // Remove remaining elements
    removed = jedis.vrem(testKey, "element1");
    assertTrue(removed);
    assertEquals(1L, jedis.vcard(testKey));

    removed = jedis.vrem(testKey, "element3");
    assertTrue(removed);
    assertEquals(0L, jedis.vcard(testKey));
  }

  /**
   * Test VREM with binary key and elements. Verifies that VREM works with byte array keys and
   * elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVremBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();

    // Add vectors using binary key and elements
    float[] vector1 = { 1.0f, 0.0f };
    float[] vector2 = { 0.0f, 1.0f };
    float[] vector3 = { 1.0f, 1.0f };

    jedis.vadd(testKey, vector1, "binary_element1".getBytes());
    jedis.vadd(testKey, vector2, "binary_element2".getBytes());
    jedis.vadd(testKey, vector3, "binary_element3".getBytes());

    // Verify initial cardinality
    assertEquals(3L, jedis.vcard(testKey));

    // Remove element using binary VREM
    boolean removed = jedis.vrem(testKey, "binary_element2".getBytes());
    assertTrue(removed);
    assertEquals(2L, jedis.vcard(testKey));

    // Remove remaining elements using binary VREM
    boolean removed1 = jedis.vrem(testKey, "binary_element1".getBytes());
    boolean removed3 = jedis.vrem(testKey, "binary_element3".getBytes());
    assertTrue(removed1);
    assertTrue(removed3);
    assertEquals(0L, jedis.vcard(testKey));
  }

  /**
   * Test VREM with non-existent elements. Verifies that VREM handles non-existent elements
   * correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVremNonExistent(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:nonexistent";

    // Add one vector
    float[] vector = { 1.0f, 2.0f };
    jedis.vadd(testKey, vector, "existing_element");

    // Try to remove non-existent element
    boolean removed = jedis.vrem(testKey, "non_existent_element");
    assertFalse(removed);
    assertEquals(1L, jedis.vcard(testKey)); // Cardinality should remain unchanged

    // Try to remove from non-existent vector set
    String nonExistentKey = testInfo.getDisplayName() + ":non:existent:key";
    removed = jedis.vrem(nonExistentKey, "any_element");
    assertFalse(removed);
  }

  /**
   * Test VSETATTR with empty attributes (attribute deletion). Verifies that setting empty
   * attributes removes them.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsetattrDelete(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    String elementId = "point:DELETE_ATTR";
    float[] vector = { 1.0f, 2.0f };

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
   * Test VINFO command functionality. Verifies that vector set information can be retrieved.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVinfo(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    float[] vector1 = { 1.0f, 2.0f };
    float[] vector2 = { 3.0f, 4.0f };

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
    assertThat(info.getVSetUid(), greaterThan(0L));
    assertEquals(0L, info.getProjectionInputDim());
    assertEquals(1L, info.getAttributesCount());
    assertNotNull(info.getMaxLevel());
  }

  /**
   * Test VINFO with empty vector set. Verifies behavior when vector set doesn't exist.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVinfoNotExistingSet(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:empty:vector:set";

    VectorInfo info = jedis.vinfo(testKey);
    assertNull(info);
  }

  /**
   * Test VCARD command functionality. Verifies that vector set cardinality can be retrieved
   * correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVcard(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";
    float[] vector1 = { 1.0f, 2.0f };
    float[] vector2 = { 3.0f, 4.0f };

    // Initially, cardinality should be 0 for non-existent vector set
    assertEquals(0L, jedis.vcard(testKey));

    // Add first element
    boolean result1 = jedis.vadd(testKey, vector1, "element1");
    assertTrue(result1);
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(1L, jedis.vcard(testKey.getBytes()));

    // Add second element
    boolean result2 = jedis.vadd(testKey, vector2, "element2");
    assertTrue(result2);
    assertEquals(2L, jedis.vcard(testKey));
    assertEquals(2L, jedis.vcard(testKey.getBytes()));

    // Try to add duplicate element (should not increase cardinality)
    boolean result3 = jedis.vadd(testKey, vector1, "element1");
    assertFalse(result3); // Should return false for duplicate
    assertEquals(2L, jedis.vcard(testKey)); // Cardinality should remain 3
    assertEquals(2L, jedis.vcard(testKey.getBytes()));

    // Remove an element
    boolean removed = jedis.vrem(testKey, "element2");
    assertTrue(removed);
    assertEquals(1L, jedis.vcard(testKey));
    assertEquals(1L, jedis.vcard(testKey.getBytes()));

    // Remove last element
    removed = jedis.vrem(testKey, "element1");
    assertTrue(removed);
    assertEquals(0L, jedis.vcard(testKey));
    assertEquals(0L, jedis.vcard(testKey.getBytes()));

  }

  /**
   * Test VCARD with non-existent vector set.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVcardNotExistingSet(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:empty:vector:set";

    // VCARD should return 0 for non-existent vector set
    assertEquals(0L, jedis.vcard(testKey));
  }

  /**
   * Test VDIM command functionality. Verifies that vector set dimension can be retrieved correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVdim(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    // Add 2D vector
    float[] vector2D = { 1.0f, 2.0f };
    boolean result = jedis.vadd(testKey, vector2D, "element1");
    assertTrue(result);
    assertEquals(2L, jedis.vdim(testKey));
    assertEquals(2L, jedis.vdim(testKey.getBytes()));

    // Test different dimensions
    String testKey3D = testInfo.getDisplayName() + ":test:vector:set:3d";
    float[] vector3D = { 1.0f, 2.0f, 3.0f };
    jedis.vadd(testKey3D, vector3D, "element3d");
    assertEquals(3L, jedis.vdim(testKey3D));
    assertEquals(3L, jedis.vdim(testKey3D.getBytes()));

  }

  @Test
  @SinceRedisVersion("8.0.0")
  public void testVdimNotExistingSet(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:empty:vector:set";

    JedisDataException thrown = assertThrows(JedisDataException.class, () -> jedis.vdim(testKey));
    assertThat(thrown.getMessage(), is("ERR key does not exist"));

    thrown = assertThrows(JedisDataException.class, () -> jedis.vdim(testKey.getBytes()));
    assertThat(thrown.getMessage(), is("ERR key does not exist"));
  }

  // Test VDIM with empty set
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVdimWithEmptySet(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:empty:vector:set";
    // Add 2D vector
    float[] vector2D = { 1.0f, 2.0f };
    assertTrue(jedis.vadd(testKey, vector2D, "element1"));
    assertTrue(jedis.vrem(testKey, "element1"));
    assertEquals(0L, (jedis.vcard(testKey)));

    JedisDataException thrown = assertThrows(JedisDataException.class, () -> jedis.vdim(testKey));
    assertThat(thrown.getMessage(), is("ERR key does not exist"));

    thrown = assertThrows(JedisDataException.class, () -> jedis.vdim(testKey.getBytes()));
    assertThat(thrown.getMessage(), is("ERR key does not exist"));
  }

  /**
   * Test VDIM with dimension reduction. Verifies that VDIM returns the reduced dimension when
   * REDUCE is used.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVdimWithDimensionReduction(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:reduced";

    // Add 4D vector with dimension reduction to 2D
    float[] vector4D = { 1.0f, 2.0f, 3.0f, 4.0f };
    VAddParams params = new VAddParams();

    boolean result = jedis.vadd(testKey, vector4D, "element_reduced", 2, params);
    assertTrue(result);

    // VDIM should return the reduced dimension (2), not the original (4)
    assertEquals(2L, jedis.vdim(testKey));
    assertEquals(2L, jedis.vdim(testKey.getBytes()));
  }

  /**
   * Test VEMB command functionality. Verifies that vector embeddings can be retrieved correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVemb(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    // Add vector to the set
    float[] originalVector = { 1.0f, 2.0f };
    VAddParams params = new VAddParams().noQuant();
    boolean result = jedis.vadd(testKey, originalVector, "element1", params);
    assertTrue(result);

    // Retrieve the vector using VEMB
    List<Double> retrievedVector = jedis.vemb(testKey, "element1");
    assertNotNull(retrievedVector);
    assertEquals(2, retrievedVector.size());

    // Verify vector values (with small tolerance for floating point precision)
    assertEquals(1.0f, retrievedVector.get(0), 0.001);
    assertEquals(2.0f, retrievedVector.get(1), 0.001);
  }

  /**
   * Test VEMB with binary key and element. Verifies that VEMB works with byte array keys and
   * elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVembBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();
    byte[] elementId = "binary_element".getBytes();

    // Add vector to the set using binary key and element
    float[] originalVector = { 0.0f, 1.0f };
    boolean result = jedis.vadd(testKey, originalVector, elementId);
    assertTrue(result);

    // Retrieve the vector using binary VEMB
    List<Double> retrievedVector = jedis.vemb(testKey, elementId);
    assertNotNull(retrievedVector);
    assertEquals(2, retrievedVector.size());

    // Verify vector values
    assertEquals(0.0, retrievedVector.get(0), 0.001);
    assertEquals(1.0, retrievedVector.get(1), 0.001);
  }

  /**
   * Test VEMB with RAW option. Verifies that VEMB can return raw vector data when RAW flag is used
   * with FP32 format.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVembRaw(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:raw";

    // Add vector to the set using FP32 format
    float[] originalVector = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
    byte[] vectorBlob = floatArrayToFP32Bytes(originalVector);
    VAddParams params = new VAddParams().noQuant();
    boolean result = jedis.vaddFP32(testKey, vectorBlob, "raw_element", params);
    assertTrue(result);

    // Retrieve the vector using VEMB with RAW option
    RawVector rawVector = jedis.vembRaw(testKey, "raw_element");
    assertNotNull(rawVector);

    // Verify the raw data length matches the original vector length
    byte[] rawData = rawVector.getRawData();
    int expectedLength = originalVector.length * 4; // 4 bytes per float
    assertEquals(expectedLength, rawData.length);

    // Verify the quantization type is FP32
    assertEquals("f32", rawVector.getQuantizationType());

    // Verify the norm is present (L2 norm of the vector)
    assertNotNull(rawVector.getNorm());
    assertTrue(rawVector.getNorm() > 0);

    // Verify the raw data contains the correct float values by converting back
    // IEEE 754 32-bit floats are stored in little-endian format
    List<Float> reconstructedVector = VectorTestUtils.fp32BytesToFloatArray(rawData);

    // Verify the reconstructed vector matches the original
    assertEquals(originalVector.length, reconstructedVector.size());
    for (int i = 0; i < originalVector.length; i++) {
      assertEquals(originalVector[i] / rawVector.getNorm(), reconstructedVector.get(i), 0.001f);
    }
  }

  /**
   * Test VEMB with RAW option. Verifies that VEMB can return raw vector data when RAW flag is used
   * with FP32 format.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVembRawBinary(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:raw";

    // Add vector to the set using FP32 format
    float[] originalVector = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
    byte[] vectorBlob = floatArrayToFP32Bytes(originalVector);
    VAddParams params = new VAddParams().noQuant();
    boolean result = jedis.vaddFP32(testKey, vectorBlob, "raw_element", params);
    assertTrue(result);

    // Retrieve the vector using VEMB with RAW option
    RawVector rawVector = jedis.vembRaw(testKey.getBytes(), "raw_element".getBytes());
    assertNotNull(rawVector);

    // Verify the raw data length matches the original vector length
    byte[] rawData = rawVector.getRawData();
    int expectedLength = originalVector.length * 4; // 4 bytes per float
    assertEquals(expectedLength, rawData.length);

    // Verify the quantization type is FP32
    assertEquals("f32", rawVector.getQuantizationType());

    // Verify the norm is present (L2 norm of the vector)
    assertNotNull(rawVector.getNorm());
    assertTrue(rawVector.getNorm() > 0);

    // Verify the raw data contains the correct float values by converting back
    // IEEE 754 32-bit floats are stored in little-endian format
    List<Float> reconstructedVector = VectorTestUtils.fp32BytesToFloatArray(rawData);

    // Verify the reconstructed vector matches the original
    assertEquals(originalVector.length, reconstructedVector.size());
    for (int i = 0; i < originalVector.length; i++) {
      assertEquals(originalVector[i] / rawVector.getNorm(), reconstructedVector.get(i), 0.001f);
    }
  }

  /**
   * Test VEMB with non-existent element. Verifies that VEMB handles non-existent elements
   * correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVembNonExistent(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:nonexistent";

    // Add a vector first
    float[] vector = { 1.0f, 2.0f };
    jedis.vadd(testKey, vector, "existing_element");

    // Try to retrieve non-existent element
    assertNull(jedis.vemb(testKey, "non_existent_element"));

  }

  /**
   * Test VSIM command functionality. Verifies vector similarity search with vectors and elements.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsim(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set";

    setupVSimTestSet(testKey);

    // Test vsim with vector
    List<String> similar = jedis.vsim(testKey, new float[] { 0.15f, 0.25f, 0.35f });
    assertNotNull(similar);
    assertThat(similar, is(not(empty())));
    assertThat(similar.size(), is(4));
    assertThat(similar, hasItems("element1", "element2", "element3", "element4"));

    // Test vsim with element
    similar = jedis.vsimByElement(testKey, "element1");
    assertNotNull(similar);
    assertThat(similar, is(not(empty())));
    assertThat(similar.size(), is(4));
    assertThat(similar, hasItems("element1", "element2", "element3", "element4"));

    // Test vsim with vector and parameters
    VSimParams params = new VSimParams().count(2);
    similar = jedis.vsim(testKey, new float[] { 0.15f, 0.25f, 0.35f }, params);
    assertNotNull(similar);
    assertThat(similar.size(), is(2));

    // Test vsim with element and parameters
    similar = jedis.vsimByElement(testKey, "element1", params);
    assertNotNull(similar);
    assertThat(similar.size(), is(2));
  }

  private void setupVSimTestSet(String testKey) {
    // Add test vectors
    float[] vector1 = { 0.1f, 0.2f, 0.3f };
    float[] vector2 = { 0.2f, 0.3f, 0.4f };
    float[] vector3 = { 0.3f, 0.4f, 0.5f };
    float[] vector4 = { -0.1f, -0.2f, -0.3f };

    jedis.vadd(testKey, vector1, "element1");
    jedis.vadd(testKey, vector2, "element2");
    jedis.vadd(testKey, vector3, "element3");
    jedis.vadd(testKey, vector4, "element4");
  }

  /**
   * Test VSIM command with scores functionality. Verifies vector similarity search returns scores
   * correctly.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsimWithScores(TestInfo testInfo) {
    String testKey = testInfo.getDisplayName() + ":test:vector:set:scores";

    setupVSimTestSet(testKey);

    // Test vsim with vector and scores
    VSimParams params = new VSimParams();
    Map<String, Double> similarWithScores = jedis.vsimWithScores(testKey,
      new float[] { 0.15f, 0.25f, 0.35f }, params);
    assertThat(similarWithScores.keySet(), hasItems("element1", "element2", "element3"));
    assertThat(similarWithScores.values(), everyItem(greaterThanOrEqualTo(0.0)));

    // Test vsim with element and scores
    similarWithScores = jedis.vsimByElementWithScores(testKey, "element1", params);
    assertThat(similarWithScores.keySet(), hasItems("element1", "element2", "element3"));
    assertThat(similarWithScores.get("element1"), closeTo(1, 0.01));
    assertThat(similarWithScores.get("element4"), closeTo(0, 0.01));
    assertEquals(1.0, similarWithScores.get("element1"), 0.001);

    // Test with count parameter
    params = new VSimParams().count(2);
    similarWithScores = jedis.vsimWithScores(testKey, new float[] { 0.15f, 0.25f, 0.35f }, params);
    assertThat(similarWithScores.keySet(), hasItems("element1", "element2"));
    assertThat(similarWithScores.values(), everyItem(greaterThan(0.0)));

    // Test with epsilon parameter (distance-based filtering)
    params = new VSimParams().epsilon(0.2); // Only elements with similarity >= 0.8
    similarWithScores = jedis.vsimWithScores(testKey, new float[] { -0.1f, -0.2f, -0.3f }, params);
    assertNotNull(similarWithScores);
    assertThat(similarWithScores.keySet(), hasItems("element4"));
    // Verify all returned scores meet the epsilon threshold
    for (Double score : similarWithScores.values()) {
      assertTrue(score >= (1.0 - 0.2)); // score >= 0.8
    }
  }

  /**
   * Test VSIM command with binary keys and elements. Verifies vector similarity search works with
   * byte arrays.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsimBinary(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary").getBytes();

    setupVSimTestSetBinary(testKey);

    // Test vsim with vector (binary)
    List<byte[]> similar = jedis.vsim(testKey, new float[] { 0.15f, 0.25f, 0.35f });
    assertNotNull(similar);
    assertThat(similar, is(not(empty())));
    assertThat(similar.size(), is(3));
    assertThat(getBinaryElementNames(similar), hasItems("element1", "element2", "element3"));

    // Test vsim with element (binary)
    similar = jedis.vsimByElement(testKey, "element1".getBytes());
    assertNotNull(similar);
    assertThat(similar, is(not(empty())));
    assertThat(similar.size(), is(3));
    assertThat(getBinaryElementNames(similar), hasItems("element1", "element2", "element3"));

    // Test vsim with vector and parameters (binary)
    VSimParams params = new VSimParams().count(2);
    similar = jedis.vsim(testKey, new float[] { 0.15f, 0.25f, 0.35f }, params);
    assertNotNull(similar);
    assertThat(similar.size(), is(2));

    // Test vsim with element and parameters (binary)
    similar = jedis.vsimByElement(testKey, "element1".getBytes(), params);
    assertNotNull(similar);
    assertThat(similar.size(), is(2));
  }

  /**
   * Test VSIM command with binary keys and scores. Verifies vector similarity search returns scores
   * with binary data.
   */
  @Test
  @SinceRedisVersion("8.0.0")
  public void testVsimBinaryWithScores(TestInfo testInfo) {
    byte[] testKey = (testInfo.getDisplayName() + ":test:vector:set:binary:scores").getBytes();

    setupVSimTestSetBinary(testKey);

    // Test vsim with vector and scores (binary)
    VSimParams params = new VSimParams();
    Map<byte[], Double> similarWithScores = jedis.vsimWithScores(testKey,
      new float[] { 0.15f, 0.25f, 0.35f }, params);
    assertNotNull(similarWithScores);
    assertThat(similarWithScores, is(not(anEmptyMap())));

    // Verify scores are present and valid
    for (Map.Entry<byte[], Double> entry : similarWithScores.entrySet()) {
      assertNotNull(entry.getKey());
      assertNotNull(entry.getValue());
      assertThat(entry.getValue(), is(greaterThan(0.0)));
    }

    // Test vsim with element and scores (binary)
    similarWithScores = jedis.vsimByElementWithScores(testKey, "element1".getBytes(), params);
    assertNotNull(similarWithScores);
    assertThat(similarWithScores, is(not(anEmptyMap())));

    // Element1 should have perfect similarity with itself
    Double element1Score = getBinaryScoreForElement(similarWithScores, "element1");
    assertNotNull(element1Score);
    assertThat(element1Score, is(closeTo(1.0, 0.001)));

    // Test with count parameter (binary)
    params = new VSimParams().count(2);
    similarWithScores = jedis.vsimWithScores(testKey, new float[] { 0.15f, 0.25f, 0.35f }, params);
    assertNotNull(similarWithScores);
    assertThat(similarWithScores.size(), is(2));
  }

  /**
   * Helper method to set up test vector set for binary VSIM tests.
   */
  private void setupVSimTestSetBinary(byte[] testKey) {
    // Add test vectors - same as non-binary version
    float[] vector1 = { 0.1f, 0.2f, 0.3f };
    float[] vector2 = { 0.15f, 0.25f, 0.35f };
    float[] vector3 = { 0.9f, 0.8f, 0.7f };

    jedis.vadd(testKey, vector1, "element1".getBytes());
    jedis.vadd(testKey, vector2, "element2".getBytes());
    jedis.vadd(testKey, vector3, "element3".getBytes());
  }

  /**
   * Helper method to convert binary element list to string names for assertions.
   */
  private List<String> getBinaryElementNames(List<byte[]> binaryElements) {
    return binaryElements.stream().map(String::new).collect(java.util.stream.Collectors.toList());
  }

  /**
   * Helper method to get score for a specific element from binary score map.
   */
  private Double getBinaryScoreForElement(Map<byte[], Double> scoreMap, String elementName) {
    byte[] elementBytes = elementName.getBytes();
    for (Map.Entry<byte[], Double> entry : scoreMap.entrySet()) {
      if (java.util.Arrays.equals(entry.getKey(), elementBytes)) {
        return entry.getValue();
      }
    }
    return null;
  }

}
