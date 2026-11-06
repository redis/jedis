package redis.clients.jedis.params;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.RawableFactory;

public class SAddExParamsTest {

  private CommandArguments argsOf(SAddExParams params) {
    CommandArguments args = new CommandArguments(Protocol.Command.SADDEX);
    params.addParams(args);
    return args;
  }

  @Nested
  class AddParamsTests {

    @Test
    public void emptyParamsAddNothing() {
      assertThat(argsOf(SAddExParams.sAddExParams()), hasArgumentCount(1));
    }

    @Test
    public void mnx() {
      assertThat(argsOf(SAddExParams.sAddExParams().mnx()),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.MNX));
    }

    @Test
    public void mxx() {
      assertThat(argsOf(SAddExParams.sAddExParams().mxx()),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.MXX));
    }

    @Test
    public void conditionIsEmittedBeforeExpiration() {
      assertThat(argsOf(SAddExParams.sAddExParams().ex(100).mnx()),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.MNX, Protocol.Keyword.EX,
          RawableFactory.from(100L)));
    }

    @Test
    public void mxxWithPxAt() {
      assertThat(argsOf(SAddExParams.sAddExParams().mxx().pxAt(1700000000000L)),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.MXX, Protocol.Keyword.PXAT,
          RawableFactory.from(1700000000000L)));
    }

    @Test
    public void pxAndExAt() {
      assertThat(argsOf(SAddExParams.sAddExParams().px(2500)),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.PX, RawableFactory.from(2500L)));
      assertThat(argsOf(SAddExParams.sAddExParams().exAt(1700000000L)), hasArguments(
        Protocol.Command.SADDEX, Protocol.Keyword.EXAT, RawableFactory.from(1700000000L)));
    }

    @Test
    public void keepTtlHasNoValue() {
      assertThat(argsOf(SAddExParams.sAddExParams().keepTtl()),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.KEEPTTL));
    }

    @Test
    public void lastExpirationWins() {
      assertThat(argsOf(SAddExParams.sAddExParams().ex(1).px(2)),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.PX, RawableFactory.from(2L)));
      assertThat(argsOf(SAddExParams.sAddExParams().pxAt(5).keepTtl()),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.KEEPTTL));
    }

    @Test
    public void lastConditionWins() {
      assertThat(argsOf(SAddExParams.sAddExParams().mnx().mxx()),
        hasArguments(Protocol.Command.SADDEX, Protocol.Keyword.MXX));
    }
  }

  @Nested
  class EqualityAndHashCodeTests {

    @Test
    public void equalsAndHashCodeWithIdenticalParams() {
      SAddExParams first = SAddExParams.sAddExParams().mnx().ex(100);
      SAddExParams second = SAddExParams.sAddExParams().mnx().ex(100);
      assertEquals(first, second);
      assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equalsAndHashCodeWithEmptyParams() {
      assertEquals(SAddExParams.sAddExParams(), SAddExParams.sAddExParams());
      assertEquals(SAddExParams.sAddExParams().hashCode(), SAddExParams.sAddExParams().hashCode());
    }

    @Test
    public void notEqualWithDifferentCondition() {
      SAddExParams first = SAddExParams.sAddExParams().mnx().ex(100);
      SAddExParams second = SAddExParams.sAddExParams().mxx().ex(100);
      assertNotEquals(first, second);
      assertNotEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void notEqualWithDifferentExpiration() {
      SAddExParams first = SAddExParams.sAddExParams().mnx().ex(100);
      SAddExParams second = SAddExParams.sAddExParams().mnx().px(100);
      assertNotEquals(first, second);
      assertNotEquals(SAddExParams.sAddExParams().ex(100), SAddExParams.sAddExParams().ex(200));
    }

    @Test
    public void equalsWithNull() {
      assertFalse(SAddExParams.sAddExParams().equals(null));
    }

    @Test
    public void equalsWithSameInstance() {
      SAddExParams params = SAddExParams.sAddExParams().mxx();
      assertTrue(params.equals(params));
    }
  }
}
