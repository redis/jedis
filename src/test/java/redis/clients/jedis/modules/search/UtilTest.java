package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.search.RediSearchUtil;

import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.SchemaField;

public class UtilTest {

  @Test
  public void floatArrayToByteArray() {
    float[] floats = new float[]{0.2f};
    byte[] bytes = RediSearchUtil.toByteArray(floats);
    byte[] expected = new byte[]{-51, -52, 76, 62};
    Assert.assertArrayEquals(expected, bytes);
  }

  @Test
  public void getSchemaFieldName() {
    SchemaField field = NumericField.of("$.num").as("num");

    assertEquals("$.num", field.getFieldName().getName());
    assertEquals("num", field.getFieldName().getAttribute());

    assertEquals("$.num", field.getName());
  }
}
