package redis.clients.jedis.params;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.RawableFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static redis.clients.jedis.util.CommandArgumentsMatchers.*;

public class IncrexParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    IncrexParams first = getDefaultValue();
    IncrexParams second = getDefaultValue();
    assertTrue(first.equals(second));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    IncrexParams first = getDefaultValue();
    IncrexParams second = getDefaultValue();
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    IncrexParams first = new IncrexParams().lbound(0).ubound(100).ex(60);
    IncrexParams second = new IncrexParams().lbound(0).ubound(200).px(5000);
    assertFalse(first.equals(second));
  }

  @Test
  public void checkHashCodeVariousParams() {
    IncrexParams first = new IncrexParams().lbound(0).ubound(100).ex(60);
    IncrexParams second = new IncrexParams().lbound(0).ubound(200).px(5000);
    assertNotEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    IncrexParams first = getDefaultValue();
    assertFalse(first.equals(null));
  }

  @Test
  public void checkSaturate() {
    assertNotEquals(new IncrexParams().saturate(), new IncrexParams());
  }

  @Test
  public void checkEnx() {
    assertNotEquals(new IncrexParams().ex(60).enx(), new IncrexParams().ex(60));
  }

  @Test
  public void checkPersist() {
    assertNotEquals(new IncrexParams().persist(), new IncrexParams());
  }

  private IncrexParams getDefaultValue() {
    return new IncrexParams();
  }

  @Nested
  class AddParamsTests {

    @Test
    public void emptyParamsAddsNothing() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().addParams(args);

      assertThat(args, hasArgumentCount(1));
      assertThat(args, hasArguments(Protocol.Command.INCREX));
    }

    @Test
    public void lboundOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().lbound(0).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.LBOUND, RawableFactory.from(0L)));
    }

    @Test
    public void uboundOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().ubound(100L).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.UBOUND, RawableFactory.from(100L)));
    }

    @Test
    public void bothBounds() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().lbound(0).ubound(100).addParams(args);

      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.LBOUND,
        RawableFactory.from(0L), Keyword.UBOUND, RawableFactory.from(100L)));
    }

    @Test
    public void saturateFlag() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().saturate().addParams(args);

      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.SATURATE));
    }

    @Test
    public void exExpiry() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().ex(60).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.EX, RawableFactory.from(60L)));
    }

    @Test
    public void pxExpiry() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().px(5000).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.PX, RawableFactory.from(5000L)));
    }

    @Test
    public void exAtExpiry() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().exAt(1_700_000_000L).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.EXAT, RawableFactory.from(1_700_000_000L)));
    }

    @Test
    public void pxAtExpiry() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().pxAt(1_700_000_000_000L).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.PXAT,
        RawableFactory.from(1_700_000_000_000L)));
    }

    @Test
    public void persistFlag() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().persist().addParams(args);

      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.PERSIST));
    }

    @Test
    public void enxFlag() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().ex(60).enx().addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.EX, RawableFactory.from(60L), Keyword.ENX));
    }

    /**
     * Wire-format order: bounds &rarr; SATURATE &rarr; expiry &rarr; ENX.
     */
    @Test
    public void fullCombinationPreservesOrder() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().lbound(0).ubound(100).saturate().ex(60).enx().addParams(args);

      assertThat(args, hasArgumentCount(9));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.LBOUND, RawableFactory.from(0L),
          Keyword.UBOUND, RawableFactory.from(100L), Keyword.SATURATE, Keyword.EX,
          RawableFactory.from(60L), Keyword.ENX));
    }

    /**
     * Expiry options (EX/PX/EXAT/PXAT/PERSIST) are mutually exclusive on the wire: last call wins,
     * earlier ones are silently overwritten. This mirrors Jedis convention in
     * {@code BaseGetExParams} / {@code SetParams}.
     */
    @Test
    public void expiryIsSingleSlotLastWins() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexParams().ex(60).px(5000).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.PX, RawableFactory.from(5000L)));
    }
  }
}
