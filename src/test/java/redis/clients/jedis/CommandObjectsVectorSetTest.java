package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Argument building of the vector set commands, without a server.
 */
public class CommandObjectsVectorSetTest {

  private static final String KEY = "vset";
  private static final String ELEMENT = "elem";
  private static final int REDUCE_DIM = 2;

  /** Four little-endian FP32 bytes, i.e. a one dimensional vector holding 1.0f. */
  private static final byte[] BLOB = new byte[] { 0, 0, (byte) 0x80, 0x3f };

  private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP3);

  private static List<byte[]> rawArgs(CommandObject<?> command) {
    List<byte[]> raw = new ArrayList<>();
    for (Rawable arg : command.getArguments()) {
      raw.add(arg.getRaw());
    }
    return raw;
  }

  private static void assertVaddFP32Reduce(CommandObject<?> command, String... trailing) {
    List<byte[]> args = rawArgs(command);
    List<String> head = Arrays.asList("VADD", KEY, "REDUCE", String.valueOf(REDUCE_DIM), "FP32");

    assertEquals(head.size() + 2 + trailing.length, args.size());
    for (int i = 0; i < head.size(); i++) {
      assertEquals(head.get(i), SafeEncoder.encode(args.get(i)));
    }
    assertArrayEquals(BLOB, args.get(head.size()));
    assertEquals(ELEMENT, SafeEncoder.encode(args.get(head.size() + 1)));
    for (int i = 0; i < trailing.length; i++) {
      assertEquals(trailing[i], SafeEncoder.encode(args.get(head.size() + 2 + i)));
    }
  }

  /**
   * {@code params} is optional on every other VADD overload, and there is no {@code reduceDim}
   * overload without it, so {@code null} is the only way to ask for FP32 plus REDUCE and nothing
   * else.
   */
  @Test
  public void vaddFP32WithReduceDimAcceptsNullParams() {
    assertVaddFP32Reduce(commandObjects.vaddFP32(KEY, BLOB, ELEMENT, REDUCE_DIM, null));
  }

  @Test
  public void vaddFP32WithReduceDimAcceptsNullParamsBinary() {
    assertVaddFP32Reduce(commandObjects.vaddFP32(SafeEncoder.encode(KEY), BLOB,
      SafeEncoder.encode(ELEMENT), REDUCE_DIM, null));
  }

  @Test
  public void vaddFP32WithReduceDimKeepsParamsWhenGiven() {
    assertVaddFP32Reduce(
      commandObjects.vaddFP32(KEY, BLOB, ELEMENT, REDUCE_DIM, new VAddParams().noQuant()),
      "NOQUANT");
  }
}
