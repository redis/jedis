package redis.clients.jedis.search.querybuilder;

import redis.clients.jedis.search.Query;

/**
 * Created by mnunberg on 2/23/18.
 *
 * Base node interface
 */
public interface Node {

  enum Parenthesize {

    /**
     * Always encapsulate
     */
    ALWAYS,

    /**
     * Never encapsulate. Note that this may be ignored if parentheses are semantically required
     * (e.g. {@code @foo:(val1|val2)}. However something like {@code @foo:v1 @bar:v2} need not be
     * parenthesized.
     */

    NEVER,
    /**
     * Determine encapsulation based on number of children. If the node only has one child, it is
     * not parenthesized, if it has more than one child, it is parenthesized
     */

    DEFAULT
  }

  /**
   * Returns the string form of this node.
   *
   * @param mode Whether the string should be encapsulated in parentheses {@code (...)}
   * @return The string query.
   */
  String toString(Parenthesize mode);

  /**
   * Returns the string form of this node. This may be passed to
   * {@link redis.clients.jedis.UnifiedJedis#ftSearch(String, Query)}
   *
   * @return The query string.
   */
  @Override
  String toString();
}
