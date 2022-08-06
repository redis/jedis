package redis.clients.jedis.search.querybuilder;

import java.util.*;

public abstract class QueryNode implements Node {
  private final List<Node> children = new ArrayList<>();
  protected abstract String getJoinString();

  /**
   * Add a match criteria to this node
   * @param field The field to check. If null or empty, then any field is checked
   * @param values Values to check for.
   * @return The current node, for chaining.
   */
  public QueryNode add(String field, Value ...values) {
    children.add(new ValueNode(field, getJoinString(), values));
    return this;
  }

  /**
   * Convenience method to add a list of string values
   * @param field Field to check for
   * @param values One or more string values.
   * @return The current node, for chaining.
   */
  public QueryNode add(String field, String ...values) {
    children.add(new ValueNode(field, getJoinString(), values));
    return this;
  }

  /**
   * Add a list of values from a collection
   * @param field The field to check
   * @param values Collection of values to match
   * @return The current node for chaining.
   */
  public QueryNode add(String field, Collection<Value> values) {
    return add(field, values.toArray(new Value[0]));
  }

  /**
   * Add children nodes to this node.
   * @param nodes Children nodes to add
   * @return The current node, for chaining.
   */
  public QueryNode add(Node ...nodes) {
    children.addAll(Arrays.asList(nodes));
    return this;
  }

  protected boolean shouldUseParens(ParenMode mode) {
    if (mode == ParenMode.ALWAYS) {
      return true;
    }
    if (mode == ParenMode.NEVER) {
      return false;
    }
    return children.size() > 1;
  }

  @Override
  public String toString(ParenMode parenMode) {
    StringBuilder sb = new StringBuilder();
    StringJoiner sj = new StringJoiner(getJoinString());
    if (shouldUseParens(parenMode)) {
      sb.append('(');
    }
    for (Node n : children) {
      sj.add(n.toString(parenMode));
    }
    sb.append(sj.toString());
    if (shouldUseParens(parenMode)) {
      sb.append(')');
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(ParenMode.DEFAULT);
  }
}