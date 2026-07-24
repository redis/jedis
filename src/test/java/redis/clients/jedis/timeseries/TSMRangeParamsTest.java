package redis.clients.jedis.timeseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.AGGREGATION;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.EXCLUDEEMPTY;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.FILTER;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.WITHLABELS;
import static redis.clients.jedis.util.CommandArgumentsMatchers.containsArguments;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgument;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.SafeEncoder;

public class TSMRangeParamsTest {

  @Nested
  class ValidationTests {

    @Test
    public void aggregatorsEmptyArrayThrowsException() {
      assertThrows(IllegalArgumentException.class,
        () -> TSMRangeParams.multiRangeParams().aggregation(new AggregationType[0], 1000L));
    }

    @Test
    public void aggregatorsNullElementThrowsException() {
      assertThrows(IllegalArgumentException.class,
        () -> TSMRangeParams.multiRangeParams().aggregation(new AggregationType[] { null }, 1000L));
    }

    @Test
    public void excludeEmptyWithGroupByThrowsException() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams().filter("l=v").excludeEmpty()
          .groupBy("metric_name", "max");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      assertThrows(IllegalArgumentException.class, () -> params.addParams(args));
    }
  }

  @Nested
  class AggregationOverloadEquivalenceTests {

    @Test
    public void singleAggregationEqualsOneElementArrayAggregation() {
      TSMRangeParams single = TSMRangeParams.multiRangeParams()
          .aggregation(AggregationType.MIN, 1000L).filter("l=v");
      TSMRangeParams array = TSMRangeParams.multiRangeParams()
          .aggregation(AggregationType.of(AggregationType.MIN), 1000L).filter("l=v");
      assertEquals(single, array);
      assertEquals(single.hashCode(), array.hashCode());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void singleAggregatorWireFormat() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams()
          .aggregation(AggregationType.MIN, 1000L).filter("l=v");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      params.addParams(args);

      // Expected: TS.MRANGE - + AGGREGATION MIN 1000 FILTER l=v
      assertThat(args, hasArgumentCount(8));
      assertThat(args,
        hasArguments(TimeSeriesCommand.MRANGE, RawableFactory.from(MINUS),
          RawableFactory.from(PLUS), AGGREGATION, RawableFactory.from(AggregationType.MIN.getRaw()),
          RawableFactory.from(1000L), FILTER, RawableFactory.from("l=v")));
    }

    @Test
    public void multipleAggregatorsAreCommaJoined() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams()
          .aggregation(
            AggregationType.of(AggregationType.MIN, AggregationType.MAX, AggregationType.AVG),
            1000L)
          .filter("l=v");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      params.addParams(args);

      // Expected: TS.MRANGE - + AGGREGATION MIN,MAX,AVG 1000 FILTER l=v
      assertThat(args, hasArgumentCount(8));
      assertThat(args,
        hasArguments(TimeSeriesCommand.MRANGE, RawableFactory.from(MINUS),
          RawableFactory.from(PLUS), AGGREGATION,
          RawableFactory.from(SafeEncoder.encode("MIN,MAX,AVG")), RawableFactory.from(1000L),
          FILTER, RawableFactory.from("l=v")));
    }

    @Test
    public void noAggregationClauseWhenUnset() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams().filter("l=v");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      params.addParams(args);

      // Expected: TS.MRANGE - + FILTER l=v (no AGGREGATION)
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(TimeSeriesCommand.MRANGE, RawableFactory.from(MINUS),
        RawableFactory.from(PLUS), FILTER, RawableFactory.from("l=v")));
      assertThat(args, not(hasArgument(3, AGGREGATION)));
    }
  }

  @Nested
  class ExcludeEmptyTests {

    @Test
    public void excludeEmptyEmitsFlagOnceBeforeFilter() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams().excludeEmpty().filter("sensor=1");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      params.addParams(args);

      // Expected: TS.MRANGE - + EXCLUDEEMPTY FILTER sensor=1
      assertThat(args, hasArgumentCount(6));
      assertThat(args, hasArguments(TimeSeriesCommand.MRANGE, RawableFactory.from(MINUS),
        RawableFactory.from(PLUS), EXCLUDEEMPTY, FILTER, RawableFactory.from("sensor=1")));
      // EXCLUDEEMPTY is placed with the range options, immediately before FILTER.
      assertThat(args, hasArgument(3, EXCLUDEEMPTY));
    }

    @Test
    public void noExcludeEmptyFlagWhenUnset() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams().filter("sensor=1");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      params.addParams(args);

      // Expected: TS.MRANGE - + FILTER sensor=1 (no EXCLUDEEMPTY)
      assertThat(args, hasArgumentCount(5));
      assertThat(args, not(containsArguments(EXCLUDEEMPTY)));
    }

    @Test
    public void excludeEmptyComposesWithRangeOptions() {
      TSMRangeParams params = TSMRangeParams.multiRangeParams(1L, 500L).withLabels()
          .aggregation(AggregationType.MIN, 100L).excludeEmpty().filter("sensor=1");
      CommandArguments args = new CommandArguments(TimeSeriesCommand.MRANGE);
      params.addParams(args);

      // Expected: TS.MRANGE 1 500 WITHLABELS AGGREGATION MIN 100 EXCLUDEEMPTY FILTER sensor=1
      // EXCLUDEEMPTY composes with the other range options and is emitted once, immediately
      // before FILTER, after the aggregation clause. The response parser is unchanged.
      assertThat(args, hasArgumentCount(10));
      assertThat(args,
        hasArguments(TimeSeriesCommand.MRANGE, RawableFactory.from(1L), RawableFactory.from(500L),
          WITHLABELS, AGGREGATION, RawableFactory.from(AggregationType.MIN.getRaw()),
          RawableFactory.from(100L), EXCLUDEEMPTY, FILTER, RawableFactory.from("sensor=1")));
    }

    @Test
    public void excludeEmptyDistinguishesEquality() {
      TSMRangeParams withExclude = TSMRangeParams.multiRangeParams().excludeEmpty().filter("l=v");
      TSMRangeParams withoutExclude = TSMRangeParams.multiRangeParams().filter("l=v");
      assertNotEquals(withExclude, withoutExclude);

      TSMRangeParams withExcludeAgain = TSMRangeParams.multiRangeParams().excludeEmpty()
          .filter("l=v");
      assertEquals(withExclude, withExcludeAgain);
      assertEquals(withExclude.hashCode(), withExcludeAgain.hashCode());
    }
  }
}
