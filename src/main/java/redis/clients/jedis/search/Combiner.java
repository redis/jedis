package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import java.util.List;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.YIELD_SCORE_AS;

/**
 * Abstract combiner for combining multiple search scores. Instances are created via
 * {@link Combiners}.
 * @see Combiners
 */
@Experimental
public abstract class Combiner implements IParams {

  private final String name;
  private String scoreAlias;

  protected Combiner(String name) {
    this.name = name;
  }

  public final String getName() {
    return name;
  }

  /**
   * Set an alias for the combined score field using YIELD_SCORE_AS.
   * @param alias the field name to use for the combined score
   * @return this instance
   */
  public final Combiner as(String alias) {
    this.scoreAlias = alias;
    return this;
  }

  protected abstract List<Object> getOwnArgs();

  @Override
  public final void addParams(CommandArguments args) {
    args.add(name);

    List<Object> ownArgs = getOwnArgs();
    args.add(ownArgs.size());
    ownArgs.forEach(args::add);

    if (scoreAlias != null) {
      args.add(YIELD_SCORE_AS);
      args.add(scoreAlias);
    }
  }
}
