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
import redis.clients.jedis.args.RawableFactory;

public class LMoveMParamsTest {

  @Nested
  class ValidationTests {

    @Test
    public void countWithoutOrderingThrows() {
      LMoveMParams params = LMoveMParams.lMoveMParams().count(2);
      assertThrows(IllegalArgumentException.class,
        () -> params.addParams(new CommandArguments(Protocol.Command.LMOVEM)));
    }

    @Test
    public void exactlyWithoutOrderingThrows() {
      LMoveMParams params = LMoveMParams.lMoveMParams().exactly(3);
      assertThrows(IllegalArgumentException.class,
        () -> params.addParams(new CommandArguments(Protocol.Command.LMOVEM)));
    }

    @Test
    public void orderingWithoutSelectorThrows() {
      LMoveMParams params = LMoveMParams.lMoveMParams().obo();
      assertThrows(IllegalArgumentException.class,
        () -> params.addParams(new CommandArguments(Protocol.Command.LMOVEM)));
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
      LMoveMParams params = LMoveMParams.lMoveMParams().count(2).obo();
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args,
        hasArguments(Protocol.Command.LMOVEM, Keyword.COUNT, RawableFactory.from(2), Keyword.OBO));
    }

    @Test
    public void exactlyBulk() {
      LMoveMParams params = LMoveMParams.lMoveMParams().exactly(3).bulk();
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args, hasArguments(Protocol.Command.LMOVEM, Keyword.EXACTLY,
        RawableFactory.from(3), Keyword.BULK));
    }

    @Test
    public void selectorIsLastWins() {
      LMoveMParams params = LMoveMParams.lMoveMParams().count(2).exactly(5).bulk();
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args, hasArguments(Protocol.Command.LMOVEM, Keyword.EXACTLY,
        RawableFactory.from(5), Keyword.BULK));
    }

    @Test
    public void orderingIsLastWins() {
      LMoveMParams params = LMoveMParams.lMoveMParams().count(2).obo().bulk();
      CommandArguments args = new CommandArguments(Protocol.Command.LMOVEM);
      params.addParams(args);

      assertThat(args,
        hasArguments(Protocol.Command.LMOVEM, Keyword.COUNT, RawableFactory.from(2), Keyword.BULK));
    }
  }

  @Nested
  class EqualsHashCodeTests {

    @Test
    public void equalWhenSameConfiguration() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2).obo();
      LMoveMParams b = LMoveMParams.lMoveMParams().count(2).obo();
      assertTrue(a.equals(b));
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenSelectorDiffers() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2).obo();
      LMoveMParams b = LMoveMParams.lMoveMParams().exactly(2).obo();
      assertFalse(a.equals(b));
    }

    @Test
    public void notEqualWhenCountDiffers() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2).bulk();
      LMoveMParams b = LMoveMParams.lMoveMParams().count(3).bulk();
      assertFalse(a.equals(b));
      assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenOrderingDiffers() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2).obo();
      LMoveMParams b = LMoveMParams.lMoveMParams().count(2).bulk();
      assertFalse(a.equals(b));
    }

    @Test
    public void notEqualToNull() {
      LMoveMParams a = LMoveMParams.lMoveMParams().count(2).obo();
      assertFalse(a.equals(null));
    }
  }
}
