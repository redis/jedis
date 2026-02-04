package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.FILTER;

/**
 * FILTER operation for post-processing in FT.HYBRID command.
 */
@Experimental
public class Filter implements IParams {

  private final String expression;

  private Filter(String expression) {
    this.expression = expression;
  }

  /**
   * Create a FILTER operation.
   * @param expression the filter expression
   * @return a new Filter instance
   */
  public static Filter of(String expression) {
    return new Filter(expression);
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(FILTER);
    args.add(expression);
  }
}

