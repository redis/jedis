package redis.clients.jedis.modules.search;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.search.RediSearchUtil;

public class UtilTest {

  @Test
  public void floatArrayToByteArray() {
    float[] floats = new float[]{0.2f};
    byte[] bytes = RediSearchUtil.ToByteArray(floats);
    byte[] expected = new byte[]{-51, -52, 76, 62};
    Assert.assertArrayEquals(expected, bytes);
  }
}
