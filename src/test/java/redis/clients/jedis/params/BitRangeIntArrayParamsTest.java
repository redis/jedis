package redis.clients.jedis.params;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.params.BitRangeIntArrayParams.bitRangeIntArrayParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

public class BitRangeIntArrayParamsTest {

  /** The tokens the params add after the command name. */
  private static List<String> tokens(BitRangeIntArrayParams params) {
    CommandArguments args = new CommandArguments(Protocol.Command.BITRANGEINTARRAY);
    params.addParams(args);
    List<String> all = toStrings(args);
    return all.subList(1, all.size());
  }

  private static List<String> toStrings(CommandArguments args) {
    List<String> out = new ArrayList<>();
    for (Rawable r : args) {
      out.add(SafeEncoder.encode(r.getRaw()));
    }
    return out;
  }

  private static List<byte[]> toBytes(CommandArguments args) {
    List<byte[]> out = new ArrayList<>();
    for (Rawable r : args) {
      out.add(r.getRaw());
    }
    return out;
  }

  /** Scenario BRIA-A01: params with nothing set add no arguments. */
  @Test
  public void nothingSetAddsNoArguments() {
    assertEquals(Collections.emptyList(), tokens(bitRangeIntArrayParams()));
  }

  /** Scenario BRIA-A02: unit BIT adds BIT. */
  @Test
  public void unitBit() {
    assertEquals(Collections.singletonList("BIT"),
      tokens(bitRangeIntArrayParams().unit(BitCountOption.BIT)));
  }

  /** Scenario BRIA-A02a: unit BYTE adds BYTE. */
  @Test
  public void unitByte() {
    assertEquals(Collections.singletonList("BYTE"),
      tokens(bitRangeIntArrayParams().unit(BitCountOption.BYTE)));
  }

  /** Scenario BRIA-A03: a null unit adds no unit and clears one set before. */
  @Test
  public void nullUnitAddsNoUnit() {
    assertEquals(Collections.emptyList(), tokens(bitRangeIntArrayParams().unit(null)));
    assertEquals(Collections.emptyList(),
      tokens(bitRangeIntArrayParams().unit(BitCountOption.BIT).unit(null)));
  }

  /** Scenario BRIA-A04: limit(3) adds LIMIT 3. */
  @Test
  public void limitThree() {
    assertEquals(Arrays.asList("LIMIT", "3"), tokens(bitRangeIntArrayParams().limit(3)));
  }

  /** Scenario BRIA-A05: limit(0) is sent as LIMIT 0, not dropped. */
  @Test
  public void limitZeroIsSent() {
    assertEquals(Arrays.asList("LIMIT", "0"), tokens(bitRangeIntArrayParams().limit(0)));
  }

  /** Scenario BRIA-A06: limit(Long.MAX_VALUE) is sent unchanged. */
  @Test
  public void limitMaxValue() {
    assertEquals(Arrays.asList("LIMIT", "9223372036854775807"),
      tokens(bitRangeIntArrayParams().limit(Long.MAX_VALUE)));
  }

  /** Scenario BRIA-A07: a negative count is rejected before anything is sent. */
  @Test
  public void negativeLimitRejected() {
    assertThrows(IllegalArgumentException.class, () -> bitRangeIntArrayParams().limit(-1));
    assertThrows(IllegalArgumentException.class,
      () -> bitRangeIntArrayParams().limit(Long.MIN_VALUE));
  }

  /** Scenario BRIA-A08: zeros() adds one ZEROS, also when called twice. */
  @Test
  public void zerosOnce() {
    assertEquals(Collections.singletonList("ZEROS"), tokens(bitRangeIntArrayParams().zeros()));
    assertEquals(Collections.singletonList("ZEROS"),
      tokens(bitRangeIntArrayParams().zeros().zeros()));
  }

  /**
   * Scenario BRIA-A09: the options are emitted as unit, LIMIT count, ZEROS whatever the call order.
   */
  @Test
  public void fixedEmissionOrder() {
    assertEquals(Arrays.asList("BIT", "LIMIT", "3", "ZEROS"),
      tokens(bitRangeIntArrayParams().zeros().limit(3).unit(BitCountOption.BIT)));
  }

  /** Scenario BRIA-A10: the last unit and the last count win. */
  @Test
  public void lastCallWins() {
    assertEquals(Collections.singletonList("BYTE"),
      tokens(bitRangeIntArrayParams().unit(BitCountOption.BIT).unit(BitCountOption.BYTE)));
    assertEquals(Arrays.asList("LIMIT", "3"), tokens(bitRangeIntArrayParams().limit(1).limit(3)));
  }

  /** Scenario BRIA-A11: equals and hashCode hold over unit, count and ZEROS. */
  @Test
  public void equalsAndHashCode() {
    BitRangeIntArrayParams a = bitRangeIntArrayParams().unit(BitCountOption.BIT).limit(3).zeros();
    BitRangeIntArrayParams b = bitRangeIntArrayParams().zeros().limit(3).unit(BitCountOption.BIT);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(bitRangeIntArrayParams(), bitRangeIntArrayParams());
    assertEquals(bitRangeIntArrayParams().hashCode(), bitRangeIntArrayParams().hashCode());

    assertNotEquals(a, bitRangeIntArrayParams().unit(BitCountOption.BYTE).limit(3).zeros());
    assertNotEquals(a, bitRangeIntArrayParams().unit(BitCountOption.BIT).limit(4).zeros());
    assertNotEquals(a, bitRangeIntArrayParams().unit(BitCountOption.BIT).limit(3));
    assertNotEquals(bitRangeIntArrayParams(), bitRangeIntArrayParams().limit(0));
    assertNotEquals(a, null);
  }

  @Nested
  class CommandArgumentsTests {

    private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP2);

    /** Scenario BRIA-B01: the command, key, range and options, for String and byte[] keys. */
    @Test
    public void commandArguments() {
      BitRangeIntArrayParams params = bitRangeIntArrayParams().unit(BitCountOption.BIT).limit(3)
          .zeros();

      CommandArguments plain = commandObjects.bitrangeintarray("k", 0, -1).getArguments();
      assertEquals(Arrays.asList("BITRANGEINTARRAY", "k", "0", "-1"), toStrings(plain));

      CommandArguments withParams = commandObjects.bitrangeintarray("k", 0, -1, params)
          .getArguments();
      assertEquals(Arrays.asList("BITRANGEINTARRAY", "k", "0", "-1", "BIT", "LIMIT", "3", "ZEROS"),
        toStrings(withParams));

      byte[] key = SafeEncoder.encode("k");
      assertBytesEqual(toBytes(plain),
        toBytes(commandObjects.bitrangeintarray(key, 0, -1).getArguments()));
      assertBytesEqual(toBytes(withParams),
        toBytes(commandObjects.bitrangeintarray(key, 0, -1, params).getArguments()));
    }

    /** Scenario BRIA-B02: start Long.MIN_VALUE and end Long.MAX_VALUE are sent unchanged. */
    @Test
    public void extremeRangeSentUnchanged() {
      assertEquals(
        Arrays.asList("BITRANGEINTARRAY", "k", "-9223372036854775808", "9223372036854775807"),
        toStrings(
          commandObjects.bitrangeintarray("k", Long.MIN_VALUE, Long.MAX_VALUE).getArguments()));
    }

    private void assertBytesEqual(List<byte[]> expected, List<byte[]> actual) {
      assertEquals(expected.size(), actual.size());
      for (int i = 0; i < expected.size(); i++) {
        assertEquals(Arrays.toString(expected.get(i)), Arrays.toString(actual.get(i)),
          "argument " + i);
      }
    }
  }
}
