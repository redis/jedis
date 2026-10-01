package redis.clients.jedis.commands.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.Collections;
import java.util.List;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Smoke coverage of the BLESS family on the legacy {@code Jedis} client; behavior is covered in
 * depth by {@code BlessCommandsTestBase}.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#jedisRespVersions")
@EnabledOnCommand("BLESS")
public class BlessCommandsIT extends JedisCommandsTestBase {

  public BlessCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void blessLifecycle() {
    jedis.set("session:alice", "v");

    assertEquals(1L, jedis.blessSet("session:alice", BlessFlag.NO_EVICT));
    assertEquals(Collections.singletonList("NO-EVICT"), jedis.blessGet("session:alice"));

    ScanResult<String> scan = jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT, 10);
    assertTrue(scan.isCompleteIteration());
    assertEquals(Collections.singletonList("session:alice"), scan.getResult());

    assertEquals(1L, jedis.blessClear("session:alice", BlessFlag.NO_EVICT));
    assertTrue(jedis.blessGet("session:alice").isEmpty());
    assertTrue(jedis.blessScan(SCAN_POINTER_START, BlessFlag.NO_EVICT).getResult().isEmpty());
  }

  @Test
  public void blessLifecycleBinary() {
    byte[] key = SafeEncoder.encode("session:alice");
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
      () -> jedis.blessGet("nosuchkey"));
    assertTrue(e.getMessage().contains("no such key"), e.getMessage());
  }
}
