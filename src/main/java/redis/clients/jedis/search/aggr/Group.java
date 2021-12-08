package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mnunberg on 2/22/18.
 */
public class Group {

  private final List<Reducer> reducers = new ArrayList<>();
  private final List<String> fields = new ArrayList<>();
  private Limit limit = new Limit(0, 0);

  public Group(String... fields) {
    this.fields.addAll(Arrays.asList(fields));
  }

  public Group reduce(Reducer r) {
    reducers.add(r);
    return this;
  }

  public Group limit(Limit limit) {
    this.limit = limit;
    return this;
  }

  public void addArgs(List<String> args) {
    args.add(Integer.toString(fields.size()));
    args.addAll(fields);
    for (Reducer r : reducers) {
      args.add("REDUCE");
      args.add(r.getName());
      r.addArgs(args);
      String alias = r.getAlias();
      if (alias != null && !alias.isEmpty()) {
        args.add("AS");
        args.add(alias);
      }
    }
    args.addAll(limit.getArgs());
  }

  public List<String> getArgs() {
    List<String> args = new ArrayList<>();
    addArgs(args);
    return args;
  }
}
