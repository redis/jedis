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

public class SDiffCardParamsTest {

  @Test
  public void checkEqualsIdenticalParams() {
    SDiffCardParams first = new SDiffCardParams();
    SDiffCardParams second = new SDiffCardParams();
    assertTrue(first.equals(second));
  }

  @Test
  public void checkHashCodeIdenticalParams() {
    SDiffCardParams first = new SDiffCardParams().limit(10);
    SDiffCardParams second = new SDiffCardParams().limit(10);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsVariousParams() {
    SDiffCardParams first = new SDiffCardParams().limit(10);
    SDiffCardParams second = new SDiffCardParams().limit(20);
    assertFalse(first.equals(second));
  }

  @Test
  public void checkHashCodeVariousParams() {
    SDiffCardParams first = new SDiffCardParams().limit(10);
    SDiffCardParams second = new SDiffCardParams().limit(20);
    assertNotEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void checkEqualsWithNull() {
    SDiffCardParams first = new SDiffCardParams();
    assertFalse(first.equals(null));
  }

  @Nested
  class ValidationTests {

    @Test
    public void limitAcceptsZeroAndPositive() {
      assertDoesNotThrow(() -> new SDiffCardParams().limit(0));
      assertDoesNotThrow(() -> new SDiffCardParams().limit(1000));
    }

    @Test
    public void limitRejectsNegative() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new SDiffCardParams().limit(-1));
      assertEquals("LIMIT must be non-negative", exception.getMessage());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void emptyParamsAddsNothing() {
      CommandArguments args = new CommandArguments(Protocol.Command.SDIFFCARD);
      new SDiffCardParams().addParams(args);

      assertThat(args, hasArgumentCount(1));
      assertThat(args, hasArguments(Protocol.Command.SDIFFCARD));
    }

    @Test
    public void limitOnly() {
      CommandArguments args = new CommandArguments(Protocol.Command.SDIFFCARD);
      new SDiffCardParams().limit(100).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.SDIFFCARD, Keyword.LIMIT, RawableFactory.from(100L)));
    }

    @Test
    public void limitZeroIsSent() {
      CommandArguments args = new CommandArguments(Protocol.Command.SDIFFCARD);
      new SDiffCardParams().limit(0).addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(Protocol.Command.SDIFFCARD, Keyword.LIMIT, RawableFactory.from(0L)));
    }
  }
}
