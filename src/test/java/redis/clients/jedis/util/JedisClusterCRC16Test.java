package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

public class JedisClusterCRC16Test {

  @Test
  public void testGetCRC16() {
    Map<String, Integer> solutions = prepareSolutionSet();

    for (Entry<String, Integer> entry : solutions.entrySet()) {
      // string version
      assertEquals(entry.getValue().intValue(), JedisClusterCRC16.getCRC16(entry.getKey()));

      // byte array version
      assertEquals(entry.getValue().intValue(),
        JedisClusterCRC16.getCRC16(SafeEncoder.encode(entry.getKey())));
    }
  }

  @Test
  public void testGetSlot() {
    assertEquals(7186, JedisClusterCRC16.getSlot("51"));
  }

  private Map<String, Integer> prepareSolutionSet() {
    Map<String, Integer> solutionMap = new HashMap<String, Integer>();
    solutionMap.put("", 0x0);
    solutionMap.put("123456789", 0x31C3);
    solutionMap.put("sfger132515", 0xA45C);
    solutionMap.put("hae9Napahngaikeethievubaibogiech", 0x58CE);
    solutionMap.put("AAAAAAAAAAAAAAAAAAAAAA", 0x92cd);
    solutionMap.put("Hello, World!", 0x4FD6);
    return solutionMap;
  }

  @Test
  public void testRedisHashtagGetSlot() {
    assertEquals(JedisClusterCRC16.getSlot("{bar"), JedisClusterCRC16.getSlot("foo{{bar}}zap"));
    assertEquals(JedisClusterCRC16.getSlot("{user1000}.following"),
      JedisClusterCRC16.getSlot("{user1000}.followers"));
    assertNotEquals(JedisClusterCRC16.getSlot("foo{}{bar}"), JedisClusterCRC16.getSlot("bar"));
    assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}"), JedisClusterCRC16.getSlot("bar"));
  }

  @Test
  public void testHashtagGetSlotMatchesEncodedBytesUnderDbcsCharset() {
    // GBK bytes 81 7B 41 81 7D: a lead byte followed by 0x7B ('{'), then 'A', then a lead byte
    // followed by 0x7D ('}'). The server hashes the byte-level tag between 0x7B and 0x7D, so the
    // String slot must agree with the byte slot regardless of the default charset.
    byte[] gbkBytes = new byte[] { (byte) 0x81, 0x7B, 0x41, (byte) 0x81, 0x7D };
    Charset original = SafeEncoder.DEFAULT_CHARSET;
    try {
      SafeEncoder.DEFAULT_CHARSET = Charset.forName("GBK");
      String key = new String(gbkBytes, SafeEncoder.DEFAULT_CHARSET);
      assertEquals(JedisClusterCRC16.getSlot(SafeEncoder.encode(key)),
        JedisClusterCRC16.getSlot(key));
    } finally {
      SafeEncoder.DEFAULT_CHARSET = original;
    }
  }

  @Test
  public void testHashtagGetSlotMatchesEncodedBytesUnderUtf8() {
    String[] testKeys = new String[] {
      "foo{bar}zap",
      "{user1000}.following",
      "{user1000}.followers",
      "foo{{bar}}zap",
      "foo{bar}{zap}",
      "foo{}{bar}",
      "plainkey",
      "",
      "测试{tag}测试",
      "ユーザー{user}データ"
    };

    for (String key : testKeys) {
      assertEquals(JedisClusterCRC16.getSlot(SafeEncoder.encode(key)),
        JedisClusterCRC16.getSlot(key));
    }
  }

  @Test
  public void testHashtagGetSlotMatchesEncodedBytesUnderShiftJisAndBig5() {
    Charset original = SafeEncoder.DEFAULT_CHARSET;
    try {
      // Shift_JIS: 0x81 0x7B and 0x81 0x7D are bracket characters whose trail bytes collide with '{' and '}'
      SafeEncoder.DEFAULT_CHARSET = Charset.forName("Shift_JIS");
      byte[] sjisBytes = new byte[] { (byte) 0x81, 0x7B, 0x41, (byte) 0x81, 0x7D };
      String sjisKey = new String(sjisBytes, SafeEncoder.DEFAULT_CHARSET);
      assertEquals(JedisClusterCRC16.getSlot(SafeEncoder.encode(sjisKey)),
        JedisClusterCRC16.getSlot(sjisKey));

      // Big5: 0xA4 0x7B and 0xA4 0x7D have trail bytes colliding with '{' and '}'
      SafeEncoder.DEFAULT_CHARSET = Charset.forName("Big5");
      byte[] big5Bytes = new byte[] { (byte) 0xA4, 0x7B, 0x41, (byte) 0xA4, 0x7D };
      String big5Key = new String(big5Bytes, SafeEncoder.DEFAULT_CHARSET);
      assertEquals(JedisClusterCRC16.getSlot(SafeEncoder.encode(big5Key)),
        JedisClusterCRC16.getSlot(big5Key));
    } finally {
      SafeEncoder.DEFAULT_CHARSET = original;
    }
  }

  @Test
  public void testBinaryHashtagGetSlot() {
    assertEquals(JedisClusterCRC16.getSlot("{bar".getBytes()),
      JedisClusterCRC16.getSlot("{bar".getBytes()));
    assertEquals(JedisClusterCRC16.getSlot("{user1000}.following".getBytes()),
      JedisClusterCRC16.getSlot("{user1000}.followers".getBytes()));
    assertNotEquals(JedisClusterCRC16.getSlot("foo{}{bar}".getBytes()),
      JedisClusterCRC16.getSlot("bar".getBytes()));
    assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}".getBytes()),
      JedisClusterCRC16.getSlot("bar".getBytes()));
  }

}
