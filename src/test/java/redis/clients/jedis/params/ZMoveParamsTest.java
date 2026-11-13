package redis.clients.jedis.params;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

public class ZMoveParamsTest {

  /** The tokens the params add after the command name. */
  private static List<String> tokens(ZMoveParams params) {
    List<String> out = new ArrayList<>();
    for (byte[] raw : rawTokens(params)) {
      out.add(SafeEncoder.encode(raw));
    }
    return out;
  }

  private static List<byte[]> rawTokens(ZMoveParams params) {
    CommandArguments args = new CommandArguments(Protocol.Command.ZMOVE);
    params.addParams(args);
    List<byte[]> out = new ArrayList<>();
    for (Rawable r : args) {
      out.add(r.getRaw());
    }
    return out.subList(1, out.size());
  }

  /** Scenario ZMOV-A01: {@code min()} adds MIN. */
  @Test
  public void minAddsMin() {
    assertEquals(Collections.singletonList("MIN"), tokens(ZMoveParams.min()));
  }

  /** Scenario ZMOV-A02: {@code max()} adds MAX. */
  @Test
  public void maxAddsMax() {
    assertEquals(Collections.singletonList("MAX"), tokens(ZMoveParams.max()));
  }

  /** Scenario ZMOV-A03: {@code min()} without a count adds no COUNT. */
  @Test
  public void minWithoutCountAddsNoCount() {
    assertFalse(tokens(ZMoveParams.min()).contains("COUNT"));
  }

  /** Scenario ZMOV-A04: a text score range without a count adds the bounds as given, no COUNT. */
  @Test
  public void textRangeWithoutCount() {
    assertEquals(Arrays.asList("BYSCORE", "(1", "3"), tokens(ZMoveParams.byScore("(1", "3")));
  }

  /** Scenario ZMOV-A05: infinite double bounds are sent as -inf and +inf. */
  @Test
  public void infiniteDoubleBounds() {
    double lo = Double.NEGATIVE_INFINITY;
    double hi = Double.POSITIVE_INFINITY;
    List<String> expected = Arrays.asList("BYSCORE", "-inf", "+inf", "COUNT", "2");
    assertEquals(expected, tokens(ZMoveParams.byScore(lo, hi).count(2)));
  }

  /** Scenario ZMOV-A06: double bounds are sent in Java's spelling: selection, bounds, COUNT. */
  @Test
  public void doubleBoundsSpelling() {
    List<String> decimal = tokens(ZMoveParams.byScore(1.0, 3.0).count(2));
    assertEquals(Arrays.asList("BYSCORE", "1.0", "3.0", "COUNT", "2"), decimal);
    List<String> exponent = tokens(ZMoveParams.byScore(1.0E-5, 1.0E-5));
    assertEquals(Arrays.asList("BYSCORE", "1.0E-5", "1.0E-5"), exponent);
    List<String> nan = tokens(ZMoveParams.byScore(Double.NaN, 1));
    assertEquals(Arrays.asList("BYSCORE", "NaN", "1.0"), nan);
  }

  /** Scenario ZMOV-A07: binary bounds are sent as the given bytes. */
  @Test
  public void binaryBounds() {
    byte[] min = SafeEncoder.encode("(1");
    byte[] max = SafeEncoder.encode("3");
    List<byte[]> raw = rawTokens(ZMoveParams.byScore(min, max));
    assertEquals(3, raw.size());
    assertArrayEquals(SafeEncoder.encode("BYSCORE"), raw.get(0));
    assertArrayEquals(min, raw.get(1));
    assertArrayEquals(max, raw.get(2));
  }

  /** Scenario ZMOV-A08: any count is accepted and sent as given. */
  @Test
  public void anyCountIsSent() {
    assertEquals(Arrays.asList("MIN", "COUNT", "0"), tokens(ZMoveParams.min().count(0)));
    assertEquals(Arrays.asList("MIN", "COUNT", "-1"), tokens(ZMoveParams.min().count(-1)));
    List<String> max = tokens(ZMoveParams.min().count(Long.MAX_VALUE));
    assertEquals(Arrays.asList("MIN", "COUNT", "9223372036854775807"), max);
  }

  /** Scenario ZMOV-A09: a null bound is rejected by the factory. */
  @Test
  public void nullBoundRejected() {
    byte[] one = SafeEncoder.encode("1");
    assertThrows(IllegalArgumentException.class, () -> ZMoveParams.byScore(null, "1"));
    assertThrows(IllegalArgumentException.class, () -> ZMoveParams.byScore("1", null));
    assertThrows(IllegalArgumentException.class, () -> ZMoveParams.byScore((byte[]) null, one));
    assertThrows(IllegalArgumentException.class, () -> ZMoveParams.byScore(one, (byte[]) null));
  }

  /** Scenario ZMOV-A09a: equals and hashCode hold over selection, bounds and count. */
  @Test
  public void equalsAndHashCode() {
    ZMoveParams a = ZMoveParams.min().count(2);
    ZMoveParams b = ZMoveParams.min().count(2);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    ZMoveParams r1 = ZMoveParams.byScore("(1", "3");
    ZMoveParams r2 = ZMoveParams.byScore("(1", "3");
    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
    assertEquals(ZMoveParams.byScore(1.0, 3.0), ZMoveParams.byScore(1.0, 3.0));

    assertNotEquals(ZMoveParams.min(), ZMoveParams.max());
    assertNotEquals(ZMoveParams.byScore("1", "3"), ZMoveParams.byScore("1", "4"));
    assertNotEquals(ZMoveParams.byScore("1", "3"), ZMoveParams.byScore("0", "3"));
    assertNotEquals(ZMoveParams.min().count(2), ZMoveParams.min().count(3));
    assertNotEquals(ZMoveParams.min().count(2), ZMoveParams.min());
  }
}
