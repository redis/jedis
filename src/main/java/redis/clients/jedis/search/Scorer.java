package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import java.util.List;

/**
 * Abstract scorer for text search. Instances are created via {@link Scorers}.
 * @see Scorers
 */
@Experimental
public abstract class Scorer implements IParams {

  private final String name;

  protected Scorer(String name) {
    this.name = name;
  }

  public final String getName() {
    return name;
  }

  @Override
  public final void addParams(CommandArguments args) {
    args.add(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
