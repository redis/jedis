package redis.clients.jedis.timeseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.AGGREGATION;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.FILTER;
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
}
