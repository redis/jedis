package redis.clients.jedis.graph;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;

/**
 * @deprecated Redis Graph support is deprecated.
 */
@Deprecated
public interface RedisGraphPipelineCommands {

  @Deprecated
  Response<ResultSet> graphQuery(String name, String query);

  @Deprecated
  Response<ResultSet> graphReadonlyQuery(String name, String query);

  @Deprecated
  Response<ResultSet> graphQuery(String name, String query, long timeout);

  @Deprecated
  Response<ResultSet> graphReadonlyQuery(String name, String query, long timeout);

  @Deprecated
  Response<ResultSet> graphQuery(String name, String query, Map<String, Object> params);

  @Deprecated
  Response<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params);

  @Deprecated
  Response<ResultSet> graphQuery(String name, String query, Map<String, Object> params, long timeout);

  @Deprecated
  Response<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout);

  @Deprecated
  Response<String> graphDelete(String name);

  @Deprecated
  Response<List<String>> graphProfile(String graphName, String query);
}
