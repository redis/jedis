package redis.clients.jedis.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.SchemaField;

public class UtilTest {

  @Test
  public void floatArrayToByteArray() {
    float[] floats = new float[] { 0.2f };
    byte[] bytes = RediSearchUtil.toByteArray(floats);
    byte[] expected = new byte[] { -51, -52, 76, 62 };
    assertArrayEquals(expected, bytes);
  }

  @Test
  public void escapeQueryEscapesBackslash() {
    // in a query the escape character itself must be escaped, otherwise a value's
    // own backslash consumes the backslash the escaper prepends to the next operator
    assertEquals("\\\\", RediSearchUtil.escapeQuery("\\"));
    assertEquals("a\\\\b", RediSearchUtil.escapeQuery("a\\b"));
    // the indexing path is intentionally unchanged
    assertEquals("\\", RediSearchUtil.escape("\\"));
  }

  @Test
  public void escapeQueryKeepsOperatorInert() {
    // '|' (union) must stay literal even when the value starts with a backslash
    assertEquals("\\\\\\|evil", RediSearchUtil.escapeQuery("\\|evil"));
  }

  @Test
  public void getSchemaFieldName() {
    SchemaField field = NumericField.of("$.num").as("num");

    assertEquals("$.num", field.getFieldName().getName());
    assertEquals("num", field.getFieldName().getAttribute());

    assertEquals("$.num", field.getName());
  }

}
