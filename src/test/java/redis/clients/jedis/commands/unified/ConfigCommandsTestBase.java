package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.SafeEncoder;

@Tag("integration")
public abstract class ConfigCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public ConfigCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  // configGet tests
  @Test
  public void configGetSinglePattern() {
    Map<String, String> result = jedis.configGet("maxmemory");
    assertNotNull(result);
    assertTrue(result.containsKey("maxmemory"));
  }

  @Test
  public void configGetMultiplePatterns() {
    Map<String, String> result = jedis.configGet("maxmemory", "timeout");
    assertNotNull(result);
    assertTrue(result.containsKey("maxmemory"));
    assertTrue(result.containsKey("timeout"));
  }

  @Test
  public void configGetWildcard() {
    Map<String, String> result = jedis.configGet("max*");
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  public void configGetBinarySinglePattern() {
    Map<byte[], byte[]> result = jedis.configGet(SafeEncoder.encode("maxmemory"));
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  public void configGetBinaryMultiplePatterns() {
    Map<byte[], byte[]> result = jedis.configGet(SafeEncoder.encode("maxmemory"),
      SafeEncoder.encode("timeout"));
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  // configSet tests
  @Test
  public void configSetSingleParameter() {
    // Get current value first
    Map<String, String> current = jedis.configGet("slowlog-max-len");
    String originalValue = current.get("slowlog-max-len");

    try {
      String result = jedis.configSet("slowlog-max-len", "200");
      assertEquals("OK", result);

      Map<String, String> verify = jedis.configGet("slowlog-max-len");
      assertEquals("200", verify.get("slowlog-max-len"));
    } finally {
      // Restore original value
      jedis.configSet("slowlog-max-len", originalValue);
    }
  }

  @Test
  public void configSetMultipleParametersVarargs() {
    Map<String, String> current = jedis.configGet("slowlog-max-len", "slowlog-log-slower-than");
    String origLen = current.get("slowlog-max-len");
    String origSlower = current.get("slowlog-log-slower-than");

    try {
      String result = jedis.configSet("slowlog-max-len", "150", "slowlog-log-slower-than", "20000");
      assertEquals("OK", result);

      Map<String, String> verify = jedis.configGet("slowlog-max-len", "slowlog-log-slower-than");
      assertEquals("150", verify.get("slowlog-max-len"));
      assertEquals("20000", verify.get("slowlog-log-slower-than"));
    } finally {
      jedis.configSet("slowlog-max-len", origLen, "slowlog-log-slower-than", origSlower);
    }
  }

  @Test
  public void configSetMapParameters() {
    Map<String, String> current = jedis.configGet("slowlog-max-len");
    String originalValue = current.get("slowlog-max-len");

    try {
      Map<String, String> params = new HashMap<>();
      params.put("slowlog-max-len", "180");

      String result = jedis.configSet(params);
      assertEquals("OK", result);

      Map<String, String> verify = jedis.configGet("slowlog-max-len");
      assertEquals("180", verify.get("slowlog-max-len"));
    } finally {
      jedis.configSet("slowlog-max-len", originalValue);
    }
  }

  @Test
  public void configSetBinarySingleParameter() {
    Map<String, String> current = jedis.configGet("slowlog-max-len");
    String originalValue = current.get("slowlog-max-len");

    try {
      String result = jedis.configSet(SafeEncoder.encode("slowlog-max-len"),
        SafeEncoder.encode("190"));
      assertEquals("OK", result);
    } finally {
      jedis.configSet("slowlog-max-len", originalValue);
    }
  }

  @Test
  public void configSetBinaryMultipleParametersVarargs() {
    Map<String, String> current = jedis.configGet("slowlog-max-len");
    String origLen = current.get("slowlog-max-len");

    try {
      String result = jedis.configSet(SafeEncoder.encode("slowlog-max-len"),
        SafeEncoder.encode("160"));
      assertEquals("OK", result);
    } finally {
      jedis.configSet("slowlog-max-len", origLen);
    }
  }

  @Test
  public void configSetBinaryMapParameters() {
    Map<String, String> current = jedis.configGet("slowlog-max-len");
    String originalValue = current.get("slowlog-max-len");

    try {
      Map<byte[], byte[]> params = new HashMap<>();
      params.put(SafeEncoder.encode("slowlog-max-len"), SafeEncoder.encode("170"));

      String result = jedis.configSetBinary(params);
      assertEquals("OK", result);
    } finally {
      jedis.configSet("slowlog-max-len", originalValue);
    }
  }

  // configResetStat test
  @Test
  public void configResetStat() {
    String result = jedis.configResetStat();
    assertEquals("OK", result);
  }

  // configRewrite test - this requires a config file, so we just verify it doesn't throw
  @Test
  public void configRewrite() {
    // configRewrite may fail if redis was not started with a config file
    // Just verify the method is callable - it may throw an error if no config file
    try {
      jedis.configRewrite();
    } catch (Exception e) {
      // Expected if Redis was not started with a config file
      assertTrue(e.getMessage().contains("rewriting config file")
          || e.getMessage().contains("CONFIG REWRITE"));
    }
  }
}
