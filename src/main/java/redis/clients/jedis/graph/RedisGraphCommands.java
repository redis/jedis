package redis.clients.jedis.graph;

import java.util.Map;

public interface RedisGraphCommands {

  /**
   * Execute a Cypher query.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @return a result set
   */
  ResultSet graphQuery(String name, String query);

  /**
   * Execute a Cypher read-only query.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @return a result set
   */
  ResultSet graphReadonlyQuery(String name, String query);

  /**
   * Execute a Cypher query with timeout.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @param timeout
   * @return a result set
   */
  ResultSet graphQuery(String name, String query, long timeout);

  /**
   * Execute a Cypher read-only query with timeout.
   *
   * @param name a graph to perform the query on
   * @param query Cypher query
   * @param timeout
   * @return a result set
   */
  ResultSet graphReadonlyQuery(String name, String query, long timeout);

  /**
   * Executes a cypher query with parameters.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @return a result set.
   */
  ResultSet graphQuery(String name, String query, Map<String, Object> params);

  /**
   * Executes a cypher read-only query with parameters.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @return a result set.
   */
  ResultSet graphReadonlyQuery(String name, String query, Map<String, Object> params);

  /**
   * Executes a cypher query with parameters and timeout.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @param timeout
   * @return a result set.
   */
  ResultSet graphQuery(String name, String query, Map<String, Object> params, long timeout);

  /**
   * Executes a cypher read-only query with parameters and timeout.
   *
   * @param name a graph to perform the query on.
   * @param query Cypher query.
   * @param params parameters map.
   * @param timeout
   * @return a result set.
   */
  ResultSet graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout);

  /**
   * Deletes the entire graph
   *
   * @param name graph to delete
   * @return delete running time statistics
   */
  String graphDelete(String name);

}
