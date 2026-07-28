package redis.clients.jedis.params;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.ListMoveOrder;
import redis.clients.jedis.args.RawableFactory;

public class LMoveMParamsTest {

  @Nested
  class ValidationTests {

    @Test
    public void nullOrderingThrows() {
      assertThrows(IllegalArgumentException.class,
        () -> LMoveMParams.lMoveMParams().count(2, null));
      assertThrows(IllegalArgumentException.class,
        () -> LMoveMParams.lMoveMParams().exactly(3, null));
    }

    @Test
    public void emptyParamsThrows() {
      LMoveMParams params = LMoveMParams.lMoveMParams();
      assertThrows(IllegalArgumentException.class,
        () -> params.addParams(new CommandArguments(Protocol.Command.LMOVEM)));
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void countObo() {
      LMoveMParams params = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO);
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args, hasArguments(Protocol.Command.LMOVEM, Keyword.COUNT, RawableFactory.from(2),
        ListMoveOrder.OBO));
    }

    @Test
    public void exactlyBulk() {
      LMoveMParams params = LMoveMParams.lMoveMParams().exactly(3, ListMoveOrder.BULK);
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args, hasArguments(Protocol.Command.LMOVEM, Keyword.EXACTLY,
        RawableFactory.from(3), ListMoveOrder.BULK));
    }

    @Test
    public void selectorIsLastWins() {
      LMoveMParams params = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO).exactly(5,
        ListMoveOrder.BULK);
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args, hasArguments(Protocol.Command.LMOVEM, Keyword.EXACTLY,
        RawableFactory.from(5), ListMoveOrder.BULK));
    }
  }

  @Nested
  class EqualsHashCodeTests {

    @Test
    public void equalWhenSameConfiguration() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO);
      LMoveMParams b = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO);
      assertTrue(a.equals(b));
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenSelectorDiffers() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO);
      LMoveMParams b = LMoveMParams.lMoveMParams().exactly(2, ListMoveOrder.OBO);
      assertFalse(a.equals(b));
    }

    @Test
    public void notEqualWhenCountDiffers() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.BULK);
      LMoveMParams b = LMoveMParams.lMoveMParams().count(3, ListMoveOrder.BULK);
      assertFalse(a.equals(b));
      assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenOrderingDiffers() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO);
      LMoveMParams b = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.BULK);
      assertFalse(a.equals(b));
    }

    @Test
    public void notEqualToNull() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2, ListMoveOrder.OBO);
      assertFalse(a.equals(null));
    }
  }
}
