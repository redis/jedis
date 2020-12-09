package redis.clients.jedis.tests.utils;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.util.KeyMergeUtil;

public class KeyMergeUtilTest {

  @Test
  public void mergeByteArray() {
    byte[][] bytes = KeyMergeUtil.merge(new byte[] {3, 2, 1}, new byte[][] {{1, 2, 3}, {2, 4, 8}});

    Assert.assertArrayEquals(new byte[] {3, 2, 1}, ((bytes)[0]));
    Assert.assertArrayEquals(new byte[] {1, 2, 3}, ((bytes)[1]));
    Assert.assertArrayEquals(new byte[] {2, 4, 8}, ((bytes)[2]));
  }

  @Test
  public void mergeString() {
    String[] strings = KeyMergeUtil.merge("1234", new String[] {"fooBar"});

    Assert.assertEquals("1234", strings[0]);
    Assert.assertEquals("fooBar", strings[1]);
  }
}
