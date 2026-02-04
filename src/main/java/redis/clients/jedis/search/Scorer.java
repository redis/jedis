package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import java.util.List;

/**
 * Abstract scorer for text search in FT.HYBRID command. This class is normally received via
 * {@link Scorers}.
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

  protected abstract List<Object> getOwnArgs();

  @Override
  public final void addParams(CommandArguments args) {
    args.add(name);
    getOwnArgs().forEach(args::add);
  }
}

