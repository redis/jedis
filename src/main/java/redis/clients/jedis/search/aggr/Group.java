package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mnunberg on 2/22/18.
 */
public class Group {

  private final List<String> fields = new ArrayList<>();
  private final List<Reducer> reducers = new ArrayList<>();

  public Group(String... fields) {
    this.fields.addAll(Arrays.asList(fields));
  }

  public Group reduce(Reducer r) {
    reducers.add(r);
    return this;
  }

  public void addArgs(List<Object> args) {

    args.add(fields.size());
    args.addAll(fields);

    reducers.forEach((r) -> r.addArgs(args));
  }
}
