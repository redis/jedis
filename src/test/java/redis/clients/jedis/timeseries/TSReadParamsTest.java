package redis.clients.jedis.timeseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.DOLLAR;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.BLOCK;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.MAX_COUNT;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;

public class TSReadParamsTest {

  private static CommandArguments args() {
    return new CommandArguments(TimeSeriesCommand.READ);
  }

  @Nested
  class ValidationTests {

    @Test
    public void negativeBlockMillisecondsThrowsException() {
      assertThrows(IllegalArgumentException.class, () -> TSReadParams.readParams().block(-1L, 1));
    }

    @Test
    public void zeroBlockMillisecondsIsAllowed() {
      // BLOCK 0 means "wait indefinitely" and must be supported.
      assertTrue(TSReadParams.readParams().block(0L, 1).isBlocking());
    }

    @Test
    public void nonPositiveBlockMinCountThrowsException() {
      assertThrows(IllegalArgumentException.class, () -> TSReadParams.readParams().block(1000L, 0));
      assertThrows(IllegalArgumentException.class,
        () -> TSReadParams.readParams().block(1000L, -3));
    }

    @Test
    public void nonPositiveMaxCountThrowsException() {
      assertThrows(IllegalArgumentException.class, () -> TSReadParams.readParams().maxCount(0));
      assertThrows(IllegalArgumentException.class, () -> TSReadParams.readParams().maxCount(-2));
    }

    @Test
    public void minCountGreaterThanMaxCountThrowsAtSerialization() {
      TSReadParams params = TSReadParams.readParams().block(1000L, 5).maxCount(2);
      assertThrows(IllegalArgumentException.class, () -> params.addParams(args()));
    }
  }

  @Nested
  class BlockingClassificationTests {

    @Test
    public void notBlockingByDefault() {
      assertFalse(TSReadParams.readParams().isBlocking());
      assertFalse(TSReadParams.readParams().timestamp(100L).maxCount(10).isBlocking());
    }

    @Test
    public void blockingWhenBlockGroupPresent() {
      assertTrue(TSReadParams.readParams().block(1000L, 1).isBlocking());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void defaultsToEarliestCursor() {
      TSReadParams params = TSReadParams.readParams();
      CommandArguments args = args();
      params.addParams(args);

      // Expected: TS.READ -
      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(MINUS)));
    }

    @Test
    public void literalCursorZeroIsEmitted() {
      TSReadParams params = TSReadParams.readParams().timestamp(0L);
      CommandArguments args = args();
      params.addParams(args);

      // Expected: TS.READ 0
      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(0L)));
    }

    @Test
    public void latestSentinel() {
      CommandArguments args = args();
      TSReadParams.readParams().latest().addParams(args);

      // Expected: TS.READ +
      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(PLUS)));
    }

    @Test
    public void newSamplesSentinel() {
      CommandArguments args = args();
      TSReadParams.readParams().newSamples().addParams(args);

      // Expected: TS.READ $
      assertThat(args, hasArgumentCount(2));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(DOLLAR)));
    }

    @Test
    public void blockGroupEmitsKeywordAndBothValues() {
      CommandArguments args = args();
      TSReadParams.readParams().timestamp(101L).block(1000L, 10).addParams(args);

      // Expected: TS.READ 101 BLOCK 1000 10
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(101L), BLOCK,
        RawableFactory.from(1000L), RawableFactory.from(10L)));
    }

    @Test
    public void maxCountEmitsKeywordAndValue() {
      CommandArguments args = args();
      TSReadParams.readParams().timestamp(200L).maxCount(2).addParams(args);

      // Expected: TS.READ 200 MAX_COUNT 2
      assertThat(args, hasArgumentCount(4));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(200L), MAX_COUNT,
        RawableFactory.from(2L)));
    }

    @Test
    public void canonicalOrderBlockBeforeMaxCount() {
      CommandArguments args = args();
      TSReadParams.readParams().newSamples().block(5000L, 1).maxCount(100).addParams(args);

      // Expected: TS.READ $ BLOCK 5000 1 MAX_COUNT 100
      assertThat(args, hasArgumentCount(7));
      assertThat(args, hasArguments(TimeSeriesCommand.READ, RawableFactory.from(DOLLAR), BLOCK,
        RawableFactory.from(5000L), RawableFactory.from(1L), MAX_COUNT, RawableFactory.from(100L)));
    }
  }

  @Nested
  class EqualsHashCodeTests {

    @Test
    public void equalWhenSameConfiguration() {
      TSReadParams a = TSReadParams.readParams().timestamp(100L).block(1000L, 5).maxCount(10);
      TSReadParams b = TSReadParams.readParams().timestamp(100L).block(1000L, 5).maxCount(10);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenCursorDiffers() {
      assertNotEquals(TSReadParams.readParams().timestamp(100L),
        TSReadParams.readParams().timestamp(200L));
      assertNotEquals(TSReadParams.readParams().earliest(), TSReadParams.readParams().latest());
    }

    @Test
    public void notEqualWhenBlockDiffers() {
      assertNotEquals(TSReadParams.readParams().block(1000L, 1),
        TSReadParams.readParams().block(2000L, 1));
    }
  }
}
