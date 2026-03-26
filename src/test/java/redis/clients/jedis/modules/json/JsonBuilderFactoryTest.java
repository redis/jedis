package redis.clients.jedis.modules.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.json.JsonBuilderFactory;

/**
 * Unit tests for JsonBuilderFactory, specifically for the JSON_NUMBER_LIST builder that preserves
 * numeric types (Long for integers, Double for decimals).
 */
public class JsonBuilderFactoryTest {

  @Test
  public void jsonNumberListPreservesLongTypes() {
    // Simulates RESP3 response where Redis returns Long for integers
    List<Object> input = Arrays.asList(1L, 2L, 3L, 100L);
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(4, result.size());
    for (Number n : result) {
      assertTrue(n instanceof Long, "Expected Long but got " + n.getClass().getName());
    }
    assertEquals(1L, result.get(0));
    assertEquals(2L, result.get(1));
    assertEquals(3L, result.get(2));
    assertEquals(100L, result.get(3));
  }

  @Test
  public void jsonNumberListPreservesDoubleTypes() {
    // Simulates RESP3 response where Redis returns Double for decimals
    List<Object> input = Arrays.asList(1.5, 2.5, 3.14159, 100.0);
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(4, result.size());
    for (Number n : result) {
      assertTrue(n instanceof Double, "Expected Double but got " + n.getClass().getName());
    }
    assertEquals(1.5, result.get(0));
    assertEquals(2.5, result.get(1));
    assertEquals(3.14159, result.get(2));
    assertEquals(100.0, result.get(3));
  }

  @Test
  public void jsonNumberListPreservesMixedTypes() {
    // Simulates RESP3 response with mixed Long and Double values
    List<Object> input = Arrays.asList(1L, 2.5, 3L, 4.0);
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(4, result.size());
    assertTrue(result.get(0) instanceof Long, "First element should be Long");
    assertTrue(result.get(1) instanceof Double, "Second element should be Double");
    assertTrue(result.get(2) instanceof Long, "Third element should be Long");
    assertTrue(result.get(3) instanceof Double, "Fourth element should be Double");

    assertEquals(1L, result.get(0));
    assertEquals(2.5, result.get(1));
    assertEquals(3L, result.get(2));
    assertEquals(4.0, result.get(3));
  }

  @Test
  public void jsonNumberListHandlesNullElements() {
    // Simulates response with null values (for non-numeric JSON paths)
    List<Object> input = Arrays.asList(null, 1L, null, 2.5);
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(4, result.size());
    assertNull(result.get(0));
    assertEquals(1L, result.get(1));
    assertNull(result.get(2));
    assertEquals(2.5, result.get(3));
  }

  @Test
  public void jsonNumberListHandlesNullInput() {
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(null);
    assertNull(result);
  }

  @Test
  public void jsonNumberListParsesStringIntegers() {
    // Simulates RESP2 response where numbers come as byte arrays/strings
    List<Object> input = Arrays.asList("1".getBytes(), "2".getBytes(), "100".getBytes());
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(3, result.size());
    for (Number n : result) {
      assertTrue(n instanceof Long, "Parsed integer string should be Long");
    }
    assertEquals(1L, result.get(0));
    assertEquals(2L, result.get(1));
    assertEquals(100L, result.get(2));
  }

  @Test
  public void jsonNumberListParsesStringDecimals() {
    // Simulates RESP2 response where decimal numbers come as byte arrays/strings
    List<Object> input = Arrays.asList("1.5".getBytes(), "2.5".getBytes(), "3.14159".getBytes());
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(3, result.size());
    for (Number n : result) {
      assertTrue(n instanceof Double, "Parsed decimal string should be Double");
    }
    assertEquals(1.5, result.get(0));
    assertEquals(2.5, result.get(1));
    assertEquals(3.14159, result.get(2));
  }

  @Test
  public void jsonNumberListParsesScientificNotation() {
    // Test parsing of scientific notation numbers
    List<Object> input = Arrays.asList("1e10".getBytes(), "2.5E-3".getBytes());
    List<Number> result = JsonBuilderFactory.JSON_NUMBER_LIST.build(input);

    assertEquals(2, result.size());
    assertTrue(result.get(0) instanceof Double, "Scientific notation should be Double");
    assertTrue(result.get(1) instanceof Double, "Scientific notation should be Double");
    assertEquals(1e10, result.get(0));
    assertEquals(2.5e-3, result.get(1));
  }

  @Test
  public void jsonArrayOrDoubleListReturnsNumberListForListInput() {
    // Test that JSON_ARRAY_OR_DOUBLE_LIST uses JSON_NUMBER_LIST for List input
    List<Object> input = Arrays.asList(1L, 2.5, 3L);
    Object result = JsonBuilderFactory.JSON_ARRAY_OR_DOUBLE_LIST.build(input);

    assertTrue(result instanceof List, "Result should be a List");
    @SuppressWarnings("unchecked")
    List<Number> numberList = (List<Number>) result;

    assertEquals(3, numberList.size());
    assertTrue(numberList.get(0) instanceof Long, "First element should be Long");
    assertTrue(numberList.get(1) instanceof Double, "Second element should be Double");
    assertTrue(numberList.get(2) instanceof Long, "Third element should be Long");
  }
}
