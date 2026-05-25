package redis.clients.jedis.params;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.util.SafeEncoder;

public class ArgrepParamsTest {

  private static final Rawable MIN = RawableFactory.from(new byte[] { '-' });
  private static final Rawable MAX = RawableFactory.from(new byte[] { '+' });

  @Test
  public void checkEqualsIdenticalParams() {
    ArgrepParams a = ArgrepParams.range(0L, 10L).match("foo");
    ArgrepParams b = ArgrepParams.range(0L, 10L).match("foo");
    assertTrue(a.equals(b));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    ArgrepParams a = ArgrepParams.range(0L, 10L).match("foo");
    ArgrepParams b = ArgrepParams.range(0L, 10L).match("foo");
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void checkEqualsDifferentBounds() {
    ArgrepParams a = ArgrepParams.range(0L, 10L).match("foo");
    ArgrepParams b = ArgrepParams.range(0L, 11L).match("foo");
    assertFalse(a.equals(b));
    assertNotEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void checkEqualsDifferentPredicates() {
    ArgrepParams a = ArgrepParams.range(0L, 10L).match("foo");
    ArgrepParams b = ArgrepParams.range(0L, 10L).match("bar");
    assertFalse(a.equals(b));
  }

  @Test
  public void checkEqualsWithNull() {
    assertFalse(ArgrepParams.unbounded().equals(null));
  }

  @Nested
  class FactoryBoundsTests {

    @Test
    public void unboundedEmitsMinAndMax() {
      assertWireBounds(ArgrepParams.unbounded(), MIN, MAX);
    }

    @Test
    public void unboundedReversedSwapsToMaxMin() {
      assertWireBounds(ArgrepParams.unbounded().reversed(), MAX, MIN);
    }

    @Test
    public void rangeEmitsNumericStartEnd() {
      assertWireBounds(ArgrepParams.range(3L, 7L), RawableFactory.from(3L),
        RawableFactory.from(7L));
    }

    @Test
    public void rangeReversedSwapsStartEnd() {
      assertWireBounds(ArgrepParams.range(3L, 7L).reversed(), RawableFactory.from(7L),
        RawableFactory.from(3L));
    }

    @Test
    public void fromEmitsNumericStartAndMax() {
      assertWireBounds(ArgrepParams.from(5L), RawableFactory.from(5L), MAX);
    }

    @Test
    public void fromReversedSwapsToMaxAndNumeric() {
      assertWireBounds(ArgrepParams.from(5L).reversed(), MAX, RawableFactory.from(5L));
    }

    @Test
    public void toEmitsMinAndNumericEnd() {
      assertWireBounds(ArgrepParams.to(5L), MIN, RawableFactory.from(5L));
    }

    @Test
    public void toReversedSwapsToNumericAndMin() {
      assertWireBounds(ArgrepParams.to(5L).reversed(), RawableFactory.from(5L), MIN);
    }

    private void assertWireBounds(ArgrepParams params, Rawable start, Rawable end) {
      CommandArguments args = new CommandArguments(Protocol.Command.ARGREP);
      params.addParams(args);
      assertThat(args, hasArguments(Protocol.Command.ARGREP, start, end));
    }
  }

  @Nested
  class WireOrderTests {

    @Test
    public void predicatesAreEmittedAfterBounds() {
      ArgrepParams params = ArgrepParams.range(0L, 10L).match("foo");
      CommandArguments args = new CommandArguments(Protocol.Command.ARGREP);
      params.addParams(args);
      assertThat(args, hasArguments(Protocol.Command.ARGREP, RawableFactory.from(0L),
        RawableFactory.from(10L), Keyword.MATCH, bytes("foo")));
    }

    @Test
    public void combinatorLimitAndNocaseFollowPredicates() {
      ArgrepParams params = ArgrepParams.range(0L, 10L).match("a").match("b").and().limit(5L)
          .nocase();
      CommandArguments args = new CommandArguments(Protocol.Command.ARGREP);
      params.addParams(args);
      assertThat(args,
        hasArguments(Protocol.Command.ARGREP, RawableFactory.from(0L), RawableFactory.from(10L),
          Keyword.MATCH, bytes("a"), Keyword.MATCH, bytes("b"), Keyword.AND, Keyword.LIMIT,
          RawableFactory.from(5L), Keyword.NOCASE));
    }

    @Test
    public void exactGlobAndReKeywordsAreEmitted() {
      ArgrepParams params = ArgrepParams.unbounded().exact("x").glob("g*").re("[0-9]+").or();
      CommandArguments args = new CommandArguments(Protocol.Command.ARGREP);
      params.addParams(args);
      assertThat(args, hasArguments(Protocol.Command.ARGREP, MIN, MAX, Keyword.EXACT, bytes("x"),
        Keyword.GLOB, bytes("g*"), Keyword.RE, bytes("[0-9]+"), Keyword.OR));
    }

    private Rawable bytes(String s) {
      return RawableFactory.from(SafeEncoder.encode(s));
    }

    @Test
    public void noArgConstructorIsPrivate() throws NoSuchMethodException {
      assertTrue(java.lang.reflect.Modifier
          .isPrivate(ArgrepParams.class.getDeclaredConstructor().getModifiers()));
    }
  }
}
