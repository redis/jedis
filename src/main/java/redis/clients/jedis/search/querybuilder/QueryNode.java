package redis.clients.jedis.search.querybuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

public abstract class QueryNode implements Node {

  private final List<Node> children = new ArrayList<>();

  protected abstract String getJoinString();

  /**
   * Add a match criteria to this node
   *
   * @param field The field to check. If null or empty, then any field is checked
   * @param values Values to check for.
   * @return The current node, for chaining.
   */
  public QueryNode add(String field, Value... values) {
    children.add(new ValueNode(field, getJoinString(), values));
    return this;
  }

  /**
   * Convenience method to add a list of string values
   *
   * @param field Field to check for
   * @param values One or more string values.
   * @return The current node, for chaining.
   */
  public QueryNode add(String field, String... values) {
    children.add(new ValueNode(field, getJoinString(), values));
    return this;
  }

  /**
   * Add a list of values from a collection
   *
   * @param field The field to check
   * @param values Collection of values to match
   * @return The current node for chaining.
   */
  public QueryNode add(String field, Collection<Value> values) {
    return add(field, values.toArray(new Value[0]));
  }

  /**
   * Add children nodes to this node.
   *
   * @param nodes Children nodes to add
   * @return The current node, for chaining.
   */
  public QueryNode add(Node... nodes) {
    children.addAll(Arrays.asList(nodes));
    return this;
  }

  protected boolean shouldParenthesize(Parenthesize mode) {
    if (mode == Parenthesize.ALWAYS) {
      return true;
    } else if (mode == Parenthesize.NEVER) {
      return false;
    } else {
      return children.size() > 1;
    }
  }

  @Override
  public String toString(Parenthesize parenMode) {
    StringBuilder sb = new StringBuilder();
    StringJoiner sj = new StringJoiner(getJoinString());
    if (shouldParenthesize(parenMode)) {
      sb.append('(');
    }
    for (Node n : children) {
      sj.add(n.toString(parenMode));
    }
    sb.append(sj.toString());
    if (shouldParenthesize(parenMode)) {
      sb.append(')');
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(Parenthesize.DEFAULT);
  }
}
