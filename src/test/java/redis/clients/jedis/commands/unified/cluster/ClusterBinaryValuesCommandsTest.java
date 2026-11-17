package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.params.MSetExParams;
import redis.clients.jedis.util.SafeEncoder;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public ClusterBinaryValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Disabled
  @Override
  public void mget() {
  }

  @Disabled
  @Override
  public void mset() {
  }

  @Disabled
  @Override
  public void msetnx() {
  }

  static Stream<Arguments> msetexCrossSlotParams() {
    return Stream.of(Arguments.of("unconditional", MSetExParams.setParams().ex(60)),
      Arguments.of("NX", MSetExParams.setParams().nx().ex(60)),
      Arguments.of("XX", MSetExParams.setParams().xx().ex(60)));
  }

  @ParameterizedTest(name = "MSETEX {0} rejects cross-slot binary keys")
  @MethodSource("msetexCrossSlotParams")
  @EnabledOnCommand("MSETEX")
  public void msetexRejectsCrossSlotKeysBeforeWrites(String condition, MSetExParams params) {
    byte[] existing = SafeEncoder.encode("{a}msetex:existing");
    byte[] missing = SafeEncoder.encode("{b}msetex:missing");
    byte[] oldValue = { 0, (byte) 0xff };
    byte[] newValue = { 1, (byte) 0xfe };
    jedis.set(existing, oldValue);

    assertThrows(JedisClusterOperationException.class,
      () -> jedis.msetex(params, missing, newValue, existing, newValue));

    assertArrayEquals(oldValue, jedis.get(existing));
    assertEquals(-1L, jedis.ttl(existing));
    assertNull(jedis.get(missing));
  }

  static Stream<Arguments> msetexSameSlotConditions() {
    return Stream.of(Arguments.of("NX", MSetExParams.setParams().nx().ex(60)),
      Arguments.of("XX", MSetExParams.setParams().xx().ex(60)));
  }

  @ParameterizedTest(name = "MSETEX {0} checks all same-slot binary keys")
  @MethodSource("msetexSameSlotConditions")
  @EnabledOnCommand("MSETEX")
  public void msetexSameSlotConditionFailureLeavesKeysUnchanged(String condition,
      MSetExParams params) {
    byte[] existing = SafeEncoder.encode("{a}msetex:existing");
    byte[] missing = SafeEncoder.encode("{a}msetex:missing");
    byte[] oldValue = { 0, (byte) 0xff };
    byte[] newValue = { 1, (byte) 0xfe };
    jedis.set(existing, oldValue);

    assertFalse(jedis.msetex(params, missing, newValue, existing, newValue));

    assertArrayEquals(oldValue, jedis.get(existing));
    assertEquals(-1L, jedis.ttl(existing));
    assertNull(jedis.get(missing));
  }

  @Test
  @EnabledOnCommand("MSETEX")
  public void msetexSameSlotWithoutConditionSetsAllKeys() {
    byte[] k1 = SafeEncoder.encode("{a}msetex:first");
    byte[] k2 = SafeEncoder.encode("{a}msetex:second");
    byte[] v1 = { 0, (byte) 0xff };
    byte[] v2 = { 1, (byte) 0xfe };

    assertTrue(jedis.msetex(MSetExParams.setParams().ex(60), k1, v1, k2, v2));

    assertArrayEquals(v1, jedis.get(k1));
    assertArrayEquals(v2, jedis.get(k2));
    assertTrue(jedis.ttl(k1) > 0);
    assertTrue(jedis.ttl(k2) > 0);
  }
}
