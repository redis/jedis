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
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;

public class TSAddParamsTest {

  private static CommandArguments args() {
    return new CommandArguments(TimeSeriesCommand.ADD);
  }

  @Nested
  class AddParamsTests {

    @Test
    public void emptyParamsAddsNothing() {
      CommandArguments args = args();
      TSAddParams.addParams().addParams(args);

      assertThat(args, hasArgumentCount(1));
      assertThat(args, hasArguments(TimeSeriesCommand.ADD));
    }

    @Test
    public void duplicatePolicyEmittedOnce() {
      CommandArguments args = args();
      TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST).addParams(args);

      // Expected: TS.ADD DUPLICATE_POLICY LAST (not duplicated)
      assertThat(args, hasArgumentCount(3));
      assertThat(args, hasArguments(TimeSeriesCommand.ADD, DUPLICATE_POLICY, DuplicatePolicy.LAST));
    }

    @Test
    public void onDuplicateIndependentOfDuplicatePolicy() {
      CommandArguments args = args();
      TSAddParams.addParams().duplicatePolicy(DuplicatePolicy.LAST)
          .onDuplicate(DuplicatePolicy.FIRST).addParams(args);

      // Expected: TS.ADD DUPLICATE_POLICY LAST ON_DUPLICATE FIRST
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(TimeSeriesCommand.ADD, DUPLICATE_POLICY, DuplicatePolicy.LAST,
        ON_DUPLICATE, DuplicatePolicy.FIRST));
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
