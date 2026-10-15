package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.SafeEncoder;

@EnabledOnCommand("BLESS")
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class BlessCommandsIT extends JedisCommandsTestBase {

  public BlessCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessLifecycle() {
    jedis.set("session:42", "v");
    assertEquals(1L, jedis.blessSet("session:42", BlessFlag.NO_EVICT));
    assertThat(jedis.blessGet("session:42"), contains("NO-EVICT"));

    ScanResult<String> page = jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT,
      10);
    assertTrue(page.isCompleteIteration());
    assertThat(page.getResult(), contains("session:42"));

    assertEquals(1L, jedis.blessClear("session:42", BlessFlag.NO_EVICT));
    assertThat(jedis.blessGet("session:42"), empty());
    assertThat(jedis.blessScan(ScanParams.SCAN_POINTER_START, BlessFlag.NO_EVICT).getResult(),
      empty());
  }

  @Test
  public void blessLifecycleBinary() {
    byte[] key = SafeEncoder.encode("session:bin");
    jedis.set(key, SafeEncoder.encode("v"));
    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertArrayEquals(SafeEncoder.encode("NO-EVICT"), jedis.blessGet(key).get(0));

    ScanResult<byte[]> page = jedis.blessScan(ScanParams.SCAN_POINTER_START_BINARY,
      BlessFlag.NO_EVICT, 10);
    assertTrue(page.isCompleteIteration());
    assertArrayEquals(key, page.getResult().get(0));
    assertEquals(1,
      jedis.blessScan(ScanParams.SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT).getResult().size());

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void missingKeyIsAnError() {
    assertThrows(JedisDataException.class, () -> jedis.blessSet("missing", BlessFlag.NO_EVICT));
    assertThrows(JedisDataException.class, () -> jedis.blessClear("missing", BlessFlag.NO_EVICT));
    assertThrows(JedisDataException.class, () -> jedis.blessGet("missing"));
  }
}
