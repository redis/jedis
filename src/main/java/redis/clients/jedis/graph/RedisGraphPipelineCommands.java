package redis.clients.jedis.graph;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;

public interface RedisGraphPipelineCommands {

  Response<ResultSet> graphQuery(String name, String query);

  Response<ResultSet> graphReadonlyQuery(String name, String query);

  Response<ResultSet> graphQuery(String name, String query, long timeout);

  Response<ResultSet> graphReadonlyQuery(String name, String query, long timeout);

  Response<ResultSet> graphQuery(String name, String query, Map<String, Object> params);

  Response<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params);

  Response<ResultSet> graphQuery(String name, String query, Map<String, Object> params, long timeout);

  Response<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout);

  Response<String> graphDelete(String name);

  Response<List<String>> graphProfile(String graphName, String query);
}
