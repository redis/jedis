package redis.clients.jedis.graph;

import java.util.List;
import java.util.Map;

/**
 * @deprecated Redis Graph support is deprecated.
 */
@Deprecated
public interface RedisGraphCommands {

  /**
   * Execute a Cypher query.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @return a result set
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphQuery(String name, String query);

  /**
   * Execute a Cypher read-only query.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @return a result set
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphReadonlyQuery(String name, String query);

  /**
   * Execute a Cypher query with timeout.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @param timeout
   * @return a result set
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphQuery(String name, String query, long timeout);

  /**
   * Execute a Cypher read-only query with timeout.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @param timeout
   * @return a result set
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphReadonlyQuery(String name, String query, long timeout);

  /**
   * Executes a cypher query with parameters.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @return a result set.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphQuery(String name, String query, Map<String, Object> params);

  /**
   * Executes a cypher read-only query with parameters.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @return a result set.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphReadonlyQuery(String name, String query, Map<String, Object> params);

  /**
   * Executes a cypher query with parameters and timeout.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @param timeout
   * @return a result set.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphQuery(String name, String query, Map<String, Object> params, long timeout);

  /**
   * Executes a cypher read-only query with parameters and timeout.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @param timeout
   * @return a result set.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  ResultSet graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout);

  /**
   * Deletes the entire graph
   *
   * @param name graph to delete
   * @return delete running time statistics
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  String graphDelete(String name);

  /**
   * Lists all graph keys in the keyspace.
   * @return graph keys
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  List<String> graphList();

  /**
   * Executes a query and produces an execution plan augmented with metrics for each operation's execution.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  List<String> graphProfile(String graphName, String query);

  /**
   * Constructs a query execution plan but does not run it. Inspect this execution plan to better understand how your
   * query will get executed.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  List<String> graphExplain(String graphName, String query);

  /**
   * Returns a list containing up to 10 of the slowest queries issued against the given graph ID.
   * @deprecated Redis Graph support is deprecated.
   */
  @Deprecated
  List<List<Object>> graphSlowlog(String graphName);

  @Deprecated
  String graphConfigSet(String configName, Object value);

  @Deprecated
  Map<String, Object> graphConfigGet(String configName);
}
