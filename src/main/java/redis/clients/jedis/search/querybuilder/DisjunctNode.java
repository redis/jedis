package redis.clients.jedis.search.querybuilder;

/**
 * A disjunct node. evaluates to true if any of its children are false. Conversely, this node
 * evaluates to false only iff <b>all</b> of its children are true, making it the exact inverse of
 * {@link IntersectNode}
 *
 * In RS, it looks like:
 *
 * {@code -(@f1:v1 @f2:v2)}
 *
 * @see DisjunctUnionNode which evalutes to true if <b>all</b> its children are false.
 */
public class DisjunctNode extends IntersectNode {
  @Override
  public String toString(Parenthesize mode) {
    String ret = super.toString(Parenthesize.NEVER);
    if (shouldParenthesize(mode)) {
      return "-(" + ret + ")";
    } else {
      return "-" + ret;
    }
  }
}
