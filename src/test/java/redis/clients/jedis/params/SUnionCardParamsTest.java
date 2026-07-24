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

public class SUnionCardParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    SUnionCardParams first = new SUnionCardParams();
    SUnionCardParams second = new SUnionCardParams();
    assertTrue(first.equals(second));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    SUnionCardParams first = new SUnionCardParams().approx().limit(10);
    SUnionCardParams second = new SUnionCardParams().approx().limit(10);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    SUnionCardParams first = new SUnionCardParams().approx().limit(10);
    SUnionCardParams second = new SUnionCardParams().limit(20);
    assertFalse(first.equals(second));
  }

  @Test
  public void checkHashCodeVariousParams() {
    SUnionCardParams first = new SUnionCardParams().approx().limit(10);
    SUnionCardParams second = new SUnionCardParams().limit(20);
    assertNotEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    SUnionCardParams first = new SUnionCardParams();
    assertFalse(first.equals(null));
  }

  @Test
  public void checkApprox() {
    assertNotEquals(new SUnionCardParams().approx(), new SUnionCardParams());
  }

  @Nested
  class ValidationTests {

    @Test
    public void limitAcceptsZeroAndPositive() {
      assertDoesNotThrow(() -> new SUnionCardParams().limit(0));
      assertDoesNotThrow(() -> new SUnionCardParams().limit(1000));
    }

    @Test
    public void limitRejectsNegative() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new SUnionCardParams().limit(-1));
      assertEquals("LIMIT must be non-negative", exception.getMessage());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void emptyParamsAddsNothing() {
      CommandArguments args = new CommandArguments(Protocol.Command.SUNIONCARD);
      new SUnionCardParams().addParams(args);

      assertThat(args, hasArgumentCount(1));
      assertThat(args, hasArguments(Protocol.Command.SUNIONCARD));
    }

    @Test
    public void approxOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.SUNIONCARD);
      new SUnionCardParams().approx().addParams(args);

      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(Protocol.Command.SUNIONCARD, Keyword.APPROX));
    }

    @Test
    public void limitOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.SUNIONCARD);
      new SUnionCardParams().limit(100).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.SUNIONCARD, Keyword.LIMIT, RawableFactory.from(100L)));
    }

    @Test
    public void approxBeforeLimit() {
      CommandArguments args = new CommandArguments(Protocol.Command.SUNIONCARD);
      new SUnionCardParams().limit(100).approx().addParams(args);

      assertThat(args, hasArgumentCount(4));
      assertThat(args, hasArguments(Protocol.Command.SUNIONCARD, Keyword.APPROX, Keyword.LIMIT,
        RawableFactory.from(100L)));
    }

    @Test
    public void limitZeroIsSent() {
      CommandArguments args = new CommandArguments(Protocol.Command.SUNIONCARD);
      new SUnionCardParams().limit(0).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.SUNIONCARD, Keyword.LIMIT, RawableFactory.from(0L)));
    }
  }
}
