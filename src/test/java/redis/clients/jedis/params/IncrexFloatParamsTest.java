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

public class IncrexFloatParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    IncrexFloatParams first = getDefaultValue();
    IncrexFloatParams second = getDefaultValue();
    assertTrue(first.equals(second));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    IncrexFloatParams first = getDefaultValue();
    IncrexFloatParams second = getDefaultValue();
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    IncrexFloatParams first = new IncrexFloatParams().lbound(0.0).ubound(100.5).ex(60);
    IncrexFloatParams second = new IncrexFloatParams().lbound(0.0).ubound(200.5).px(5000);
    assertFalse(first.equals(second));
  }

  @Test
  public void checkHashCodeVariousParams() {
    IncrexFloatParams first = new IncrexFloatParams().lbound(0.0).ubound(100.5).ex(60);
    IncrexFloatParams second = new IncrexFloatParams().lbound(0.0).ubound(200.5).px(5000);
    assertNotEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    IncrexFloatParams first = getDefaultValue();
    assertFalse(first.equals(null));
  }

  @Test
  public void checkEqualsAcrossSubtypes() {
    // An IncrexParams and an IncrexFloatParams with otherwise-equal options must NOT be equal.
    // This guards against the base-class equals comparing only shared state.
    assertNotEquals(new IncrexParams().ex(60), new IncrexFloatParams().ex(60));
  }

  @Test
  public void checkSaturate() {
    assertNotEquals(new IncrexFloatParams().saturate(), new IncrexFloatParams());
  }

  @Test
  public void checkEnx() {
    assertNotEquals(new IncrexFloatParams().ex(60).enx(), new IncrexFloatParams().ex(60));
  }

  @Test
  public void checkPersist() {
    assertNotEquals(new IncrexFloatParams().persist(), new IncrexFloatParams());
  }

  private IncrexFloatParams getDefaultValue() {
    return new IncrexFloatParams();
  }

  @Nested
  class AddParamsTests {

    @Test
    public void emptyParamsAddsNothing() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().addParams(args);

      assertThat(args, hasArgumentCount(1));
      assertThat(args, hasArguments(Protocol.Command.INCREX));
    }

    @Test
    public void lboundOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().lbound(-1.5).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.LBOUND, RawableFactory.from(-1.5)));
    }

    @Test
    public void uboundOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().ubound(9.5).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.UBOUND, RawableFactory.from(9.5)));
    }

    @Test
    public void bothBounds() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().lbound(0.0).ubound(100.0).addParams(args);

      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.LBOUND,
        RawableFactory.from(0.0), Keyword.UBOUND, RawableFactory.from(100.0)));
    }

    @Test
    public void saturateFlag() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().saturate().addParams(args);

      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.SATURATE));
    }

    @Test
    public void enxFlag() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().ex(60).enx().addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.EX, RawableFactory.from(60L), Keyword.ENX));
    }

    @Test
    public void persistFlag() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().persist().addParams(args);

      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.PERSIST));
    }

    /**
     * Wire-format order: bounds &rarr; SATURATE &rarr; expiry &rarr; ENX.
     */
    @Test
    public void fullCombinationPreservesOrder() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().lbound(-1.5).ubound(9.5).saturate().ex(60).enx().addParams(args);

      assertThat(args, hasArgumentCount(9));
      assertThat(args,
        hasArguments(Protocol.Command.INCREX, Keyword.LBOUND, RawableFactory.from(-1.5),
          Keyword.UBOUND, RawableFactory.from(9.5), Keyword.SATURATE, Keyword.EX,
          RawableFactory.from(60L), Keyword.ENX));
    }

    /**
     * Expiry options (EX/PX/EXAT/PXAT/PERSIST) are mutually exclusive on the wire: last call wins,
     * earlier ones are silently overwritten. Mirrors {@link IncrexParamsTest}.
     */
    @Test
    public void expiryIsSingleSlotLastWins() {
      CommandArguments args = new CommandArguments(Protocol.Command.INCREX);
      new IncrexFloatParams().ex(60).persist().addParams(args);

      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(Protocol.Command.INCREX, Keyword.PERSIST));
    }
  }
}
