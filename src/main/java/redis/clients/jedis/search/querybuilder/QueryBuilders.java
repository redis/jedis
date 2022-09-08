package redis.clients.jedis.search.querybuilder;

import java.util.Arrays;

import static redis.clients.jedis.search.querybuilder.Values.value;

/**
 * Created by mnunberg on 2/23/18.
 *
 * This class contains methods to construct query nodes. These query nodes can be added to parent
 * query nodes (building a chain) or used as the root query node.
 */
public class QueryBuilders {
  private QueryBuilders() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Create a new intersection node with child nodes. An intersection node is true if all its
   * children are also true
   *
   * @param n sub-condition to add
   * @return The node
   */
  public static QueryNode intersect(Node... n) {
    return new IntersectNode().add(n);
  }

  /**
   * Create a new intersection node with a field-value pair.
   *
   * @param field The field that should contain this value. If this value is empty, then any field
   * will be checked.
   * @param values Value to check for. The node will be true only if the field (or any field)
   * contains <i>all</i> of the values
   * @return The node
   */
  public static QueryNode intersect(String field, Value... values) {
    return new IntersectNode().add(field, values);
  }

  /**
   * Helper method to create a new intersection node with a string value.
   *
   * @param field The field to check. If left null or empty, all fields will be checked.
   * @param stringValue The value to check
   * @return The node
   */
  public static QueryNode intersect(String field, String stringValue) {
    return intersect(field, value(stringValue));
  }

  /**
   * Create a union node. Union nodes evaluate to true if <i>any</i> of its children are true
   *
   * @param n Child node
   * @return The union node
   */
  public static QueryNode union(Node... n) {
    return new UnionNode().add(n);
  }

  /**
   * Create a union node which can match an one or more values
   *
   * @param field Field to check. If empty, all fields are checked
   * @param values Values to search for. The node evaluates to true if {@code field} matches any of
   * the values
   * @return The union node
   */
  public static QueryNode union(String field, Value... values) {
    return new UnionNode().add(field, values);
  }

  /**
   * Convenience method to match one or more strings. This is equivalent to
   * {@code union(field, value(v1), value(v2), value(v3)) ...}
   *
   * @param field Field to match
   * @param values Strings to check for
   * @return The union node
   */
  public static QueryNode union(String field, String... values) {
    return union(field, (Value[]) Arrays.stream(values).map(Values::value).toArray());
  }

  /**
   * Create a disjunct node. Disjunct nodes are true iff <b>any</b> of its children are <b>not</b>
   * true. Conversely, this node evaluates to false if <b>all</b> its children are true.
   *
   * @param n Child nodes to add
   * @return The disjunct node
   */
  public static QueryNode disjunct(Node... n) {
    return new DisjunctNode().add(n);
  }

  /**
   * Create a disjunct node using one or more values. The node will evaluate to true iff the field
   * does not match <b>any</b> of the values.
   *
   * @param field Field to check for (empty or null for any field)
   * @param values The values to check for
   * @return The node
   */
  public static QueryNode disjunct(String field, Value... values) {
    return new DisjunctNode().add(field, values);
  }

  /**
   * Create a disjunct node using one or more values. The node will evaluate to true iff the field
   * does not match <b>any</b> of the values.
   *
   * @param field Field to check for (empty or null for any field)
   * @param values The values to check for
   * @return The node
   */
  public static QueryNode disjunct(String field, String... values) {
    return disjunct(field, (Value[]) Arrays.stream(values).map(Values::value).toArray());
  }

  /**
   * Create a disjunct union node. This node evaluates to true if <b>all</b> of its children are not
   * true. Conversely, this node evaluates as false if <b>any</b> of its children are true.
   *
   * @param n
   * @return The node
   */
  public static QueryNode disjunctUnion(Node... n) {
    return new DisjunctUnionNode().add(n);
  }

  public static QueryNode disjunctUnion(String field, Value... values) {
    return new DisjunctUnionNode().add(field, values);
  }

  public static QueryNode disjunctUnion(String field, String... values) {
    return disjunctUnion(field, (Value[]) Arrays.stream(values).map(Values::value).toArray());
  }

  /**
   * Create an optional node. Optional nodes do not affect which results are returned but they
   * influence ordering and scoring.
   *
   * @param n The node to evaluate as optional
   * @return The new node
   */
  public static QueryNode optional(Node... n) {
    return new OptionalNode().add(n);
  }

  public static QueryNode optional(String field, Value... values) {
    return new OptionalNode().add(field, values);
  }
}
