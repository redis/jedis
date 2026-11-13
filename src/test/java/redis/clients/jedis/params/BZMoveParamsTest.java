package redis.clients.jedis.params;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

public class BZMoveParamsTest {

  /** The tokens the params add after the command name. */
  private static List<String> tokens(BZMoveParams params) {
    CommandArguments args = new CommandArguments(Protocol.Command.BZMOVE);
    params.addParams(args);
    List<String> out = new ArrayList<>();
    for (Rawable r : args) {
      out.add(SafeEncoder.encode(r.getRaw()));
    }
    return out.subList(1, out.size());
  }

  /** {@code min(0.5)} adds MIN 0.5. */
  @Test
  public void minWithTimeout() {
    assertEquals(Arrays.asList("MIN", "0.5"), tokens(BZMoveParams.min(0.5)));
  }

  /** End, timeout, then COUNT. */
  @Test
  public void maxWithTimeoutAndCount() {
    assertEquals(Arrays.asList("MAX", "0.0", "COUNT", "5"), tokens(BZMoveParams.max(0).count(5)));
  }

  /** The timeout keeps its fraction in Java's spelling. */
  @Test
  public void timeoutFractionKept() {
    assertEquals(Arrays.asList("MIN", "0.1"), tokens(BZMoveParams.min(0.1)));
    assertEquals(Arrays.asList("MIN", "5.0E-4"), tokens(BZMoveParams.min(0.0005)));
    assertEquals(Arrays.asList("MIN", "1.000000001"), tokens(BZMoveParams.min(1.000000001)));
  }

  /** A negative, infinite or NaN timeout is sent as given. */
  @Test
  public void outOfRangeTimeoutsAreSent() {
    assertEquals(Arrays.asList("MIN", "-1.0"), tokens(BZMoveParams.min(-1)));
    assertEquals(Arrays.asList("MIN", "+inf"), tokens(BZMoveParams.min(Double.POSITIVE_INFINITY)));
    assertEquals(Arrays.asList("MIN", "NaN"), tokens(BZMoveParams.min(Double.NaN)));
  }

  /** No count adds no COUNT. */
  @Test
  public void noCountAddsNoCount() {
    assertFalse(tokens(BZMoveParams.min(1)).contains("COUNT"));
  }

  /** Equals and hashCode hold over end, timeout and count. */
  @Test
  public void equalsAndHashCode() {
    BZMoveParams a = BZMoveParams.min(1).count(2);
    BZMoveParams b = BZMoveParams.min(1).count(2);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    assertNotEquals(a, BZMoveParams.max(1).count(2));
    assertNotEquals(a, BZMoveParams.min(2).count(2));
    assertNotEquals(a, BZMoveParams.min(1).count(3));
    assertNotEquals(a, BZMoveParams.min(1));
  }
}
