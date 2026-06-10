package redis.clients.jedis.commands.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.SafeEncoder;

@SinceRedisVersion("8.7.225")
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class ArrayCommandsTest extends JedisCommandsTestBase {

  public ArrayCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void arcountMissingKey() {
    long count = jedis.arcount("missing-array");
    assertEquals(0, count);
  }

  @Test
  public void arcountMissingKeyBinary() {
    byte[] bKey = SafeEncoder.encode("missing-array");
    long count = jedis.arcount(bKey);
    assertEquals(0, count);
  }

  @Test
  public void arlenMissingKey() {
    assertEquals(0, jedis.arlen("missing-arlen"));
  }

  @Test
  public void arnextMissingKey() {
    assertEquals(0L, jedis.arnext("missing-arnext"));
  }

  @Test
  public void arseekMissingKey() {
    assertEquals(0, jedis.arseek("missing-arseek", 0L));
  }
}
