package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.APPLY;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.AS;

/**
 * APPLY operation for search commands. Computes a new field based on an expression.
 */
@Experimental
public class Apply implements IParams {

  private final String expression;
  private final String alias;

  private Apply(String expression, String alias) {
    this.expression = expression;
    this.alias = alias;
  }

  /**
   * Create an APPLY operation.
   * @param expression the expression to apply
   * @param alias the alias for the result
   * @return a new Apply instance
   */
  public static Apply of(String expression, String alias) {
    return new Apply(expression, alias);
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(APPLY);
    args.add(expression);
    args.add(AS);
    args.add(alias);
  }
}
