package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mnunberg on 2/22/18.
 */
public class Limit {

  public static final Limit NO_LIMIT = new Limit(0, 0);

  private final int offset;
  private final int count;

  public Limit(int offset, int count) {
    this.offset = offset;
    this.count = count;
  }

  public void addArgs(List<String> args) {
    if (count == 0) {
      return;
    }
    args.add("LIMIT");
    args.add(Integer.toString(offset));
    args.add(Integer.toString(count));
  }

  public List<String> getArgs() {
    if (count == 0) {
      return Collections.emptyList();
    }
    List<String> ll = new ArrayList<>(3);
    addArgs(ll);
    return ll;
  }
}
