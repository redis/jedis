package redis.clients.jedis.search.querybuilder;

/**
 * A disjunct union node is the inverse of a {@link UnionNode}. It evaluates to true only iff
 * <b>all</b> its children are false. Conversely, it evaluates to false if <b>any</b> of its
 * children are true.
 *
 * As an RS query it looks like {@code -(@f1:v1|@f2:v2)}
 *
 * @see DisjunctNode which evaluates to true if <b>any</b> of its children are false.
 */
public class DisjunctUnionNode extends DisjunctNode {
  @Override
  protected String getJoinString() {
    return "|";
  }
}
