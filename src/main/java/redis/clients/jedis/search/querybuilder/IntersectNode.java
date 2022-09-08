package redis.clients.jedis.search.querybuilder;

/**
 * The intersection node evaluates to true if any of its children are true.
 *
 * In RS: {@code @f1:v1 @f2:v2}
 */
public class IntersectNode extends QueryNode {
  @Override
  protected String getJoinString() {
    return " ";
  }
}
