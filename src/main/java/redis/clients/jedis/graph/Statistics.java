package redis.clients.jedis.graph;

/**
 * @deprecated Redis Graph support is deprecated.
 */
@Deprecated
public interface Statistics {

  int nodesCreated();

  int nodesDeleted();

  int indicesCreated();

  int indicesDeleted();

  int labelsAdded();

  int relationshipsDeleted();

  int relationshipsCreated();

  int propertiesSet();

  boolean cachedExecution();

  String queryIntervalExecutionTime();
}
