package redis.clients.jedis.commands.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.Collections;
import java.util.List;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.TestKeyRegistry;

/**
 * Smoke coverage of the BLESS family on the legacy {@code Jedis} client; behavior is covered in
 * depth by {@code BlessCommandsTestBase}.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#jedisRespVersions")
@EnabledOnCommand("BLESS")
public class BlessCommandsIT extends JedisCommandsTestBase {

  // the legacy base has no registry; cleanup is the base class's flushAll
  private TestKeyRegistry keys;

  public BlessCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void setUpKeys(TestInfo testInfo) {
    keys = TestKeyRegistry.create(testInfo);
  }

  @Test
  public void blessLifecycle() {
    String key = keys.key("session:alice");
    jedis.set(key, "v");

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    assertEquals(Collections.singletonList(BlessFlag.NO_EVICT), jedis.blessGet(key));

    ScanResult<String> scan = jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT, 10);
    assertTrue(scan.isCompleteIteration());
    assertTrue(scan.getResult().contains(key));

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
    assertFalse(jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT).getResult().contains(key));
  }

  @Test
  public void blessLifecycleBinary() {
    byte[] key = keys.bKey("session:alice");
    jedis.set(key, SafeEncoder.encode("v"));

    assertEquals(1L, jedis.blessSet(key, BlessFlag.NO_EVICT));
    List<byte[]> flags = jedis.blessGet(key);
    assertEquals(1, flags.size());
    assertArrayEquals(SafeEncoder.encode("NO-EVICT"), flags.get(0));

    ScanResult<byte[]> scan = jedis.blessScan(SCAN_POINTER_START_BINARY, BlessFlag.NO_EVICT, 10);
    assertTrue(scan.isCompleteIteration());
    assertEquals(1, scan.getResult().size());
    assertArrayEquals(key, scan.getResult().get(0));

    assertEquals(1L, jedis.blessClear(key, BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet(key).isEmpty());
  }

  @Test
  public void blessMissingKey() {
    JedisDataException e = assertThrows(JedisDataException.class,
      () -> jedis.blessGet(keys.key("nosuchkey")));
    assertTrue(e.getMessage().contains("no such key"), e.getMessage());
  }
}
