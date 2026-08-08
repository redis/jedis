package redis.clients.jedis.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.containsArguments;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;

public class FTSearchParamsTest {

  @Test
  public void paramsMapMergesInsteadOfDroppingValues() {
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
  }
}
