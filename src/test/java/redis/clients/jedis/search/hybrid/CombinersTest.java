package redis.clients.jedis.search.hybrid;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.ALPHA;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.BETA;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.CONSTANT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.WINDOW;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.YIELD_SCORE_AS;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.search.Combiner;
import redis.clients.jedis.search.Combiners;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;

/**
 * Serialization tests for the FT.HYBRID score {@link Combiner}s built via {@link Combiners}.
 * <p>
 * Each combiner is serialized in isolation into a {@link CommandArguments} seeded with the HYBRID
 * command; the assertions cover only the combiner's own clause ({@code <method> <count> ...}).
 * </p>
 */
public class CombinersTest {

  private static CommandArguments combineArgs(Combiner combiner) {
    CommandArguments args = new CommandArguments(SearchCommand.HYBRID);
    combiner.addParams(args);
    return args;
  }

  @Nested
  class RrfTests {

    @Test
    public void rrfWithNoParamsWritesZeroCount() {
      CommandArguments args = combineArgs(Combiners.rrf());

      // Expected: RRF 0
      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(SearchCommand.HYBRID, RawableFactory.from("RRF"), RawableFactory.from(0)));
    }

    @Test
    public void rrfWithWindowOnly() {
      CommandArguments args = combineArgs(Combiners.rrf().window(20));

      // Expected: RRF 2 WINDOW 20
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("RRF"),
        RawableFactory.from(2), WINDOW, RawableFactory.from(20)));
    }

    @Test
    public void rrfWithConstantOnly() {
      CommandArguments args = combineArgs(Combiners.rrf().constant(60));

      // Expected: RRF 2 CONSTANT 60
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("RRF"),
        RawableFactory.from(2), CONSTANT, RawableFactory.from(60.0)));
    }

    @Test
    public void rrfWithWindowAndConstant() {
      CommandArguments args = combineArgs(Combiners.rrf().window(20).constant(60));

      // Expected: RRF 4 WINDOW 20 CONSTANT 60
      assertThat(args, hasArgumentCount(7));
      assertThat(args,
        hasArguments(SearchCommand.HYBRID, RawableFactory.from("RRF"), RawableFactory.from(4),
          WINDOW, RawableFactory.from(20), CONSTANT, RawableFactory.from(60.0)));
    }
  }

  @Nested
  class LinearTests {

    @Test
    public void linearWithNoParamsWritesZeroCount() {
      CommandArguments args = combineArgs(Combiners.linear());

      // Expected: LINEAR 0
      assertThat(args, hasArgumentCount(3));
      assertThat(args,
        hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"), RawableFactory.from(0)));
    }

    @Test
    public void linearWithAlphaOnly() {
      CommandArguments args = combineArgs(Combiners.linear().alpha(0.7));

      // Expected: LINEAR 2 ALPHA 0.7
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"),
        RawableFactory.from(2), ALPHA, RawableFactory.from(0.7)));
    }

    @Test
    public void linearWithBetaOnly() {
      CommandArguments args = combineArgs(Combiners.linear().beta(0.3));

      // Expected: LINEAR 2 BETA 0.3
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"),
        RawableFactory.from(2), BETA, RawableFactory.from(0.3)));
    }

    @Test
    public void linearWithWindowOnly() {
      CommandArguments args = combineArgs(Combiners.linear().window(25));

      // Expected: LINEAR 2 WINDOW 25
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"),
        RawableFactory.from(2), WINDOW, RawableFactory.from(25)));
    }

    @Test
    public void linearWithAlphaAndBeta() {
      CommandArguments args = combineArgs(Combiners.linear().alpha(0.7).beta(0.3));

      // Expected: LINEAR 4 ALPHA 0.7 BETA 0.3
      assertThat(args, hasArgumentCount(7));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"),
        RawableFactory.from(4), ALPHA, RawableFactory.from(0.7), BETA, RawableFactory.from(0.3)));
    }

    @Test
    public void linearWithAllParamsKeepsAlphaBetaWindowOrder() {
      CommandArguments args = combineArgs(Combiners.linear().alpha(0.7).beta(0.3).window(25));

      // Expected: LINEAR 6 ALPHA 0.7 BETA 0.3 WINDOW 25
      assertThat(args, hasArgumentCount(9));
      assertThat(args,
        hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"), RawableFactory.from(6),
          ALPHA, RawableFactory.from(0.7), BETA, RawableFactory.from(0.3), WINDOW,
          RawableFactory.from(25)));
    }
  }

  /**
   * The combined-score alias is appended as {@code YIELD_SCORE_AS <alias>} and must be included in
   * the clause count. Regression coverage for the off-by-two count bug where the two alias tokens
   * fell outside the count and Redis rejected them as an unknown argument.
   */
  @Nested
  class YieldScoreAsTests {

    @Test
    public void rrfWithParamsAndAliasCountsAliasTokens() {
      CommandArguments args = combineArgs(Combiners.rrf().window(20).constant(60).as("score"));

      // Expected: RRF 6 WINDOW 20 CONSTANT 60 YIELD_SCORE_AS score
      assertThat(args, hasArgumentCount(9));
      assertThat(args,
        hasArguments(SearchCommand.HYBRID, RawableFactory.from("RRF"), RawableFactory.from(6),
          WINDOW, RawableFactory.from(20), CONSTANT, RawableFactory.from(60.0), YIELD_SCORE_AS,
          RawableFactory.from("score")));
    }

    @Test
    public void rrfWithOnlyAliasCountsAliasTokens() {
      CommandArguments args = combineArgs(Combiners.rrf().as("score"));

      // Expected: RRF 2 YIELD_SCORE_AS score
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("RRF"),
        RawableFactory.from(2), YIELD_SCORE_AS, RawableFactory.from("score")));
    }

    @Test
    public void linearWithParamsAndAliasCountsAliasTokens() {
      CommandArguments args = combineArgs(Combiners.linear().alpha(0.7).beta(0.3).as("score"));

      // Expected: LINEAR 6 ALPHA 0.7 BETA 0.3 YIELD_SCORE_AS score
      assertThat(args, hasArgumentCount(9));
      assertThat(args,
        hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"), RawableFactory.from(6),
          ALPHA, RawableFactory.from(0.7), BETA, RawableFactory.from(0.3), YIELD_SCORE_AS,
          RawableFactory.from("score")));
    }

    @Test
    public void linearWithOnlyAliasCountsAliasTokens() {
      CommandArguments args = combineArgs(Combiners.linear().as("score"));

      // Expected: LINEAR 2 YIELD_SCORE_AS score
      assertThat(args, hasArgumentCount(5));
      assertThat(args, hasArguments(SearchCommand.HYBRID, RawableFactory.from("LINEAR"),
        RawableFactory.from(2), YIELD_SCORE_AS, RawableFactory.from("score")));
    }
  }
}
