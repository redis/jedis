package redis.clients.jedis.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.containsArguments;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgument;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;

public class FTSearchParamsTest {

  @Test
  public void paramsMapMergesWhenParametersAlreadySet() {
    Map<String, Object> more = new HashMap<>();
    more.put("added", "2");

    FTSearchParams params = FTSearchParams.searchParams().addParam("existing", "1").params(more);

    CommandArguments args = new CommandArguments(SearchCommand.SEARCH);
    params.addParams(args);

    assertThat(args, containsArguments("existing", "1", "added", "2"));
    assertThat(args, hasArgument(2, RawableFactory.from(4)));
  }

  @Test
  public void paramsMapMergesAcrossTwoMapCalls() {
    Map<String, Object> first = new HashMap<>();
    first.put("first", "1");
    Map<String, Object> second = new HashMap<>();
    second.put("second", "2");

    FTSearchParams params = FTSearchParams.searchParams().params(first).params(second);

    CommandArguments args = new CommandArguments(SearchCommand.SEARCH);
    params.addParams(args);

    assertThat(args, containsArguments("first", "1", "second", "2"));
    assertThat(args, hasArgument(2, RawableFactory.from(4)));
  }
}