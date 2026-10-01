package redis.clients.jedis.commands.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

@EnabledOnCommand("BLESS")
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class BlessCommandsTest extends JedisCommandsTestBase {

  public BlessCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessSetClearAndGet() {
    String key = "bless-jedis-key";
    jedis.set(key, "v");

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(1, jedis.blessGet(key).size());
    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessSetClearAndGetBinary() {
    byte[] key = SafeEncoder.encode("bless-jedis-key-b");
    jedis.set(key, SafeEncoder.encode("v"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(1, jedis.blessGet(key).size());
    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessGetMissingKeyThrows() {
    assertThrows(JedisDataException.class, () -> jedis.blessGet("bless-jedis-missing"));
  }
}
