package redis.clients.jedis.timeseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.AGGREGATION;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgument;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.SafeEncoder;

public class TSRangeParamsTest {

  @Nested
  class ValidationTests {

    @Test
    public void aggregatorsNullThrowsException() {
      assertThrows(IllegalArgumentException.class,
        () -> TSRangeParams.rangeParams().aggregation((AggregationType[]) null, 1000L));
    }

    @Test
    public void aggregatorsEmptyArrayThrowsException() {
      assertThrows(IllegalArgumentException.class,
        () -> TSRangeParams.rangeParams().aggregation(new AggregationType[0], 1000L));
    }
  }

  @Nested
  class AggregationOverloadEquivalenceTests {

    @Test
    public void singleAggregationEqualsOneElementArrayAggregation() {
      TSRangeParams single = TSRangeParams.rangeParams().aggregation(AggregationType.MIN, 1000L);
      TSRangeParams array = TSRangeParams.rangeParams()
          .aggregation(AggregationType.of(AggregationType.MIN), 1000L);
      assertEquals(single, array);
      assertEquals(single.hashCode(), array.hashCode());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void rangeParamsNoArgsDefaultsToOpenRange() {
      TSRangeParams params = TSRangeParams.rangeParams();
      CommandArguments args = new CommandArguments(TimeSeriesCommand.RANGE);
      params.addParams(args);

      // Expected: TS.RANGE - +
      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(
          TimeSeriesCommand.RANGE,
          RawableFactory.from(MINUS),
          RawableFactory.from(PLUS)));
    }

    @Test
    public void rangeParamsWithFromToWritesTimestamps() {
      TSRangeParams params = TSRangeParams.rangeParams(100L, 200L);
      CommandArguments args = new CommandArguments(TimeSeriesCommand.RANGE);
      params.addParams(args);

      // Expected: TS.RANGE 100 200
      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(
          TimeSeriesCommand.RANGE,
          RawableFactory.from(100L),
          RawableFactory.from(200L)));
    }

    @Test
    public void singleAggregatorWireFormat() {
      TSRangeParams params = TSRangeParams.rangeParams().aggregation(AggregationType.MIN, 1000L);
      CommandArguments args = new CommandArguments(TimeSeriesCommand.RANGE);
      params.addParams(args);

      // Expected: TS.RANGE - + AGGREGATION MIN 1000
      assertThat(args, hasArgumentCount(6));
      assertThat(args, hasArguments(
          TimeSeriesCommand.RANGE,
          RawableFactory.from(MINUS),
          RawableFactory.from(PLUS),
          AGGREGATION,
          RawableFactory.from(AggregationType.MIN.getRaw()),
          RawableFactory.from(1000L)));
    }

    @Test
    public void multipleAggregatorsAreCommaJoined() {
      TSRangeParams params = TSRangeParams.rangeParams().aggregation(
        AggregationType.of(AggregationType.MIN, AggregationType.MAX, AggregationType.AVG), 1000L);
      CommandArguments args = new CommandArguments(TimeSeriesCommand.RANGE);
      params.addParams(args);

      // Expected: TS.RANGE - + AGGREGATION MIN,MAX,AVG 1000
      assertThat(args, hasArgumentCount(6));
      assertThat(args, hasArguments(
          TimeSeriesCommand.RANGE,
          RawableFactory.from(MINUS),
          RawableFactory.from(PLUS),
          AGGREGATION,
          RawableFactory.from(SafeEncoder.encode("MIN,MAX,AVG")),
          RawableFactory.from(1000L)));
    }

    @Test
    public void noAggregationClauseWhenUnset() {
      TSRangeParams params = TSRangeParams.rangeParams();
      CommandArguments args = new CommandArguments(TimeSeriesCommand.RANGE);
      params.addParams(args);

      // Expected: TS.RANGE - + (no AGGREGATION)
      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(
          TimeSeriesCommand.RANGE,
          RawableFactory.from(MINUS),
          RawableFactory.from(PLUS)));
      assertThat(args, not(hasArgument(3, AGGREGATION)));
    }
  }
}
