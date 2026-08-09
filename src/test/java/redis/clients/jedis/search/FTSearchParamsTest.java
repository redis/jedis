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

    // Parameters are already present (via addParam) when the map overload is called,
    // so params(map) takes the merge branch.
    FTSearchParams params = FTSearchParams.searchParams().addParam("existing", "1").params(more);

    CommandArguments args = new CommandArguments(SearchCommand.SEARCH);
    params.addParams(args);

    // Both the pre-existing parameter and the merged-in parameter must be emitted;
    // previously the merge branch copied the field onto itself and dropped the map.
    assertThat(args, containsArguments("existing", "1", "added", "2"));
    // Pin the PARAMS arity token (two names + two values). containsArguments is
    // presence-only, so it does not by itself catch a wrong count; the arity is the
    // part the server parses.
    assertThat(args, hasArgument(2, RawableFactory.from(4)));
  }

  @Test
  public void paramsMapMergesAcrossTwoMapCalls() {
    Map<String, Object> first = new HashMap<>();
    first.put("first", "1");
    Map<String, Object> second = new HashMap<>();
    second.put("second", "2");

    // The merge branch is also reached when params(map) is called a second time;
    // the values from the first call must survive the second.
    FTSearchParams params = FTSearchParams.searchParams().params(first).params(second);

    CommandArguments args = new CommandArguments(SearchCommand.SEARCH);
    params.addParams(args);

    assertThat(args, containsArguments("first", "1", "second", "2"));
    assertThat(args, hasArgument(2, RawableFactory.from(4)));
  }
}
