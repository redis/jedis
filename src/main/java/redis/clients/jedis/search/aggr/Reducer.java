package redis.clients.jedis.search.aggr;

import java.util.List;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;

/**
 * Created by mnunberg on 2/22/18.
 *
 * This class is normally received via one of the subclasses or via Reducers
 */
public abstract class Reducer {

  private final String name;
  private final String field;
  private String alias;

  protected Reducer(String name) {
    this.name = name;
    this.field = null;
  }

  protected Reducer(String name, String field) {
    this.name = name;
    this.field = field;
  }

  public final Reducer as(String alias) {
    this.alias = alias;
    return this;
  }

  public final String getName() {
    return name;
  }

  public final String getField() {
    return field;
  }

  public final String getAlias() {
    return alias;
  }

  protected abstract List<Object> getOwnArgs();

  public final void addArgs(List<Object> args) {

    args.add(SearchKeyword.REDUCE);
    args.add(name);

    List<Object> ownArgs = getOwnArgs();
    if (field != null) {
      args.add(1 + ownArgs.size());
      args.add(field);
    } else {
      args.add(ownArgs.size());
    }
    args.addAll(ownArgs);

    if (alias != null) {
      args.add(SearchKeyword.AS);
      args.add(alias);
    }
  }
}
