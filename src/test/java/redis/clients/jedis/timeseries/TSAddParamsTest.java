package redis.clients.jedis.timeseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.DUPLICATE_POLICY;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.ON_DUPLICATE;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;

public class TSAddParamsTest {

  private static CommandArguments tsAddArgs() {
    // mirrors CommandObjects.tsAdd(key, timestamp, value, params)
    return new CommandArguments(TimeSeriesCommand.ADD).key("ts:1").add(1000L).add(25.5);
  }

  @Nested
  class AddParamsTests {

    @Test
    public void noOptionalParamsAddsNothing() {
      CommandArguments args = tsAddArgs();
      TSAddParams.addParams().addParams(args);

      // Expected: TS.ADD ts:1 1000 25.5
      assertThat(args, hasArgumentCount(4));
      assertThat(args, hasArguments(TimeSeriesCommand.ADD, RawableFactory.from("ts:1"),
        RawableFactory.from(1000L), RawableFactory.from(25.5)));
    }

    @Test
    public void duplicatePolicy() {
      CommandArguments args = tsAddArgs();
      TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST).addParams(args);

      // Expected: TS.ADD ts:1 1000 25.5 DUPLICATE_POLICY LAST
      assertThat(args, hasArgumentCount(6));
      assertThat(args,
        hasArguments(TimeSeriesCommand.ADD, RawableFactory.from("ts:1"), RawableFactory.from(1000L),
          RawableFactory.from(25.5), DUPLICATE_POLICY, DuplicatePolicy.LAST));
    }

    @Test
    public void duplicatePolicyWithOnDuplicate() {
      CommandArguments args = tsAddArgs();
      TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST)
          .onDuplicate(DuplicatePolicy.FIRST).addParams(args);

      // Expected: TS.ADD ts:1 1000 25.5 DUPLICATE_POLICY LAST ON_DUPLICATE FIRST
      assertThat(args, hasArgumentCount(8));
      assertThat(args,
        hasArguments(TimeSeriesCommand.ADD, RawableFactory.from("ts:1"), RawableFactory.from(1000L),
          RawableFactory.from(25.5), DUPLICATE_POLICY, DuplicatePolicy.LAST, ON_DUPLICATE,
          DuplicatePolicy.FIRST));
    }
  }

  @Nested
  class EqualsHashCodeTests {

    @Test
    public void equalWhenSameConfiguration() {
      TSAddParams a = TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST)
          .onDuplicate(DuplicatePolicy.FIRST);
      TSAddParams b = TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST)
          .onDuplicate(DuplicatePolicy.FIRST);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualWhenDuplicatePolicyDiffers() {
      assertNotEquals(TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST),
        TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.FIRST));
    }
  }
}
