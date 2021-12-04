package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mnunberg on 2/22/18.
 *
 * This class is normally received via one of the subclasses or via Reducers
 */
public abstract class Reducer {

  private String alias;
  private final String field;

  protected Reducer(String field) {
    this.field = field;
    this.alias = null;
  }

  protected Reducer() {
    this(null);
  }

  protected List<String> getOwnArgs() {
    if (field == null) {
      return Collections.emptyList();
    }
    List<String> ret = new ArrayList<>();
    ret.add(field);
    return ret;
  }

  /**
   * @return The name of the reducer
   */
  public abstract String getName();

  public final String getAlias() {
    return alias;
  }

  public final Reducer setAlias(String alias) {
    this.alias = alias;
    return this;
  }

  public final Reducer as(String alias) {
    return setAlias(alias);
  }

  public final Reducer setAliasAsField() {
    if (field == null || field.isEmpty()) {
      throw new IllegalArgumentException("Cannot set to field name since no field exists");
    }
    return setAlias(field);
  }

  public void addArgs(List<String> args) {
    List<String> ownArgs = getOwnArgs();
    args.add(Integer.toString(ownArgs.size()));
    args.addAll(ownArgs);
  }

  public final List<String> getArgs() {
    List<String> args = new ArrayList<>();
    addArgs(args);
    return args;
  }
}
