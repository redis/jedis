package redis.clients.jedis.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.containsArguments;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgument;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;

public class FTSearchParamsTest {

  private static final String INDEX = "idx";
  private static final String QUERY = "@price:[$min $max]";

  /** The command prefix FT.SEARCH is built with: SEARCH &lt;index&gt; &lt;query&gt;. */
  private CommandArguments searchArguments() {
    return new CommandArguments(SearchCommand.SEARCH).add(INDEX).add(QUERY);
  }

  @Nested
  class AddParamsTests {

    @Test
    public void paramsMapMergesWhenParametersAlreadySet() {
      Map<String, Object> more = new HashMap<>();
      more.put("max", "2");

      FTSearchParams params = FTSearchParams.searchParams().addParam("min", "1").params(more);

      CommandArguments args = searchArguments();
      params.addParams(args);

      // Expected: FT.SEARCH idx @price:[$min $max] PARAMS 4 <min 1 max 2 in any order>
      assertThat(args, containsArguments("min", "1", "max", "2"));
      assertThat(args, hasArgument(4, RawableFactory.from(4)));
    }

    @Test
    public void paramsMapMergesAcrossTwoMapCalls() {
      Map<String, Object> first = new HashMap<>();
      first.put("min", "1");
      Map<String, Object> second = new HashMap<>();
      second.put("max", "2");

      FTSearchParams params = FTSearchParams.searchParams().params(first).params(second);

      CommandArguments args = searchArguments();
      params.addParams(args);

      // Expected: FT.SEARCH idx @price:[$min $max] PARAMS 4 <min 1 max 2 in any order>
      assertThat(args, containsArguments("min", "1", "max", "2"));
      assertThat(args, hasArgument(4, RawableFactory.from(4)));
    }
  }
}