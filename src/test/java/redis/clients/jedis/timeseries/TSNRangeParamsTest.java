package redis.clients.jedis.timeseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.AGGREGATION;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.COUNT;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.EMPTY;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.LATEST;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgument;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.SafeEncoder;

public class TSNRangeParamsTest {

  private static CommandArguments args() {
    return new CommandArguments(TimeSeriesCommand.NRANGE);
  }

  @Nested
  class ValidationTests {

    @Test
    public void aggregatorsEmptyArrayThrows() {
      assertThrows(IllegalArgumentException.class,
        () -> TSNRangeParams.nrangeParams().aggregation(new AggregationType[0], 1000L));
    }

    @Test
    public void aggregatorsNullElementThrows() {
      assertThrows(IllegalArgumentException.class,
        () -> TSNRangeParams.nrangeParams().aggregation(new AggregationType[] { null }, 1000L));
    }

    @Test
    public void perKeyEmptyOuterThrows() {
      assertThrows(IllegalArgumentException.class,
        () -> TSNRangeParams.nrangeParams().aggregation(new AggregationType[0][], 1000L));
    }

    @Test
    public void perKeyEmptyInnerThrows() {
      assertThrows(IllegalArgumentException.class, () -> TSNRangeParams.nrangeParams()
          .aggregation(new AggregationType[][] { new AggregationType[0] }, 1000L));
    }

    @Test
    public void perKeyNullInnerElementThrows() {
      assertThrows(IllegalArgumentException.class, () -> TSNRangeParams.nrangeParams()
          .aggregation(new AggregationType[][] { { null } }, 1000L));
    }

    @Test
    public void validateAggregationForKeysThrowsOnSpecCountMismatch() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(0L, 100L)
          .aggregation(new AggregationType[] { AggregationType.AVG, AggregationType.SUM }, 1000L);
      // Two specs configured, but three keys requested.
      assertThrows(IllegalArgumentException.class, () -> params.validateAggregationForKeys(3));
    }

    @Test
    public void validateAggregationForKeysPassesWhenCountsMatch() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(0L, 100L)
          .aggregation(new AggregationType[] { AggregationType.AVG, AggregationType.SUM }, 1000L);
      params.validateAggregationForKeys(2); // must not throw
    }

    @Test
    public void validateAggregationForKeysNoOpWhenAggregationUnset() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(0L, 100L);
      params.validateAggregationForKeys(5); // no aggregation -> no constraint, must not throw
    }
  }

  @Nested
  class AggregationOverloadEquivalenceTests {

    @Test
    public void oneAggregatorPerKeyEqualsSingletonPerKey() {
      TSNRangeParams flat = TSNRangeParams.nrangeParams()
          .aggregation(new AggregationType[] { AggregationType.MIN, AggregationType.MAX }, 1000L);
      TSNRangeParams nested = TSNRangeParams.nrangeParams().aggregation(
        new AggregationType[][] { { AggregationType.MIN }, { AggregationType.MAX } }, 1000L);
      assertEquals(flat, nested);
      assertEquals(flat.hashCode(), nested.hashCode());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void noArgsDefaultsToOpenRange() {
      TSNRangeParams params = TSNRangeParams.nrangeParams();
      CommandArguments args = args();
      params.addParams(args);

      // Expected: - +
      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(TimeSeriesCommand.NRANGE, RawableFactory.from(MINUS),
        RawableFactory.from(PLUS)));
    }

    @Test
    public void fromToWritesTimestamps() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(100L, 200L);
      CommandArguments args = args();
      params.addParams(args);

      // Expected: 100 200
      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(TimeSeriesCommand.NRANGE, RawableFactory.from(100L),
        RawableFactory.from(200L)));
    }

    @Test
    public void oneAggregatorPerKeyEmitsSeparateTokens() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(0L, 60000L)
          .aggregation(new AggregationType[] { AggregationType.AVG, AggregationType.SUM }, 1000L);
      CommandArguments args = args();
      params.addParams(args);

      // Expected: 0 60000 AGGREGATION AVG SUM 1000 (one token per key, not comma-joined)
      assertThat(args, hasArgumentCount(7));
      assertThat(args,
        hasArguments(TimeSeriesCommand.NRANGE, RawableFactory.from(0L), RawableFactory.from(60000L),
          AGGREGATION, RawableFactory.from(AggregationType.AVG.getRaw()),
          RawableFactory.from(AggregationType.SUM.getRaw()), RawableFactory.from(1000L)));
    }

    @Test
    public void multipleAggregatorsPerKeyAreCommaJoinedPerKey() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(0L, 60000L)
          .aggregation(new AggregationType[][] { { AggregationType.AVG, AggregationType.MAX },
              { AggregationType.SUM } },
            1000L);
      CommandArguments args = args();
      params.addParams(args);

      // Expected: 0 60000 AGGREGATION AVG,MAX SUM 1000
      assertThat(args, hasArgumentCount(7));
      assertThat(args,
        hasArguments(TimeSeriesCommand.NRANGE, RawableFactory.from(0L), RawableFactory.from(60000L),
          AGGREGATION, RawableFactory.from(SafeEncoder.encode("AVG,MAX")),
          RawableFactory.from(AggregationType.SUM.getRaw()), RawableFactory.from(1000L)));
    }

    @Test
    public void noAggregationClauseWhenUnset() {
      TSNRangeParams params = TSNRangeParams.nrangeParams();
      CommandArguments args = args();
      params.addParams(args);

      assertThat(args, hasArgumentCount(3));
      assertThat(args, not(hasArgument(3, AGGREGATION)));
    }

    @Test
    public void fullOptionOrdering() {
      TSNRangeParams params = TSNRangeParams.nrangeParams(0L, 60000L).latest().count(5).alignStart()
          .aggregation(new AggregationType[] { AggregationType.FIRST, AggregationType.LAST }, 1000L)
          .bucketTimestampHigh().empty();
      CommandArguments args = args();
      params.addParams(args);

      // Expected: 0 60000 LATEST COUNT 5 ALIGN - AGGREGATION FIRST LAST 1000 BUCKETTIMESTAMP +
      // EMPTY
      assertThat(args,
        hasArguments(TimeSeriesCommand.NRANGE, RawableFactory.from(0L), RawableFactory.from(60000L),
          LATEST, COUNT, RawableFactory.from(5),
          redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.ALIGN,
          RawableFactory.from(MINUS), AGGREGATION,
          RawableFactory.from(AggregationType.FIRST.getRaw()),
          RawableFactory.from(AggregationType.LAST.getRaw()), RawableFactory.from(1000L),
          redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.BUCKETTIMESTAMP,
          RawableFactory.from(PLUS), EMPTY));
    }
  }

  @Nested
  class EqualsHashCodeTests {

    @Test
    public void equalWhenSameConfiguration() {
      TSNRangeParams a = TSNRangeParams.nrangeParams(0L, 100L).count(3)
          .aggregation(new AggregationType[] { AggregationType.MIN }, 10L);
      TSNRangeParams b = TSNRangeParams.nrangeParams(0L, 100L).count(3)
          .aggregation(new AggregationType[] { AggregationType.MIN }, 10L);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenAggregatorsDiffer() {
      TSNRangeParams a = TSNRangeParams.nrangeParams(0L, 100L)
          .aggregation(new AggregationType[] { AggregationType.MIN }, 10L);
      TSNRangeParams b = TSNRangeParams.nrangeParams(0L, 100L)
          .aggregation(new AggregationType[] { AggregationType.MAX }, 10L);
      assertNotEquals(a, b);
    }
  }
}
