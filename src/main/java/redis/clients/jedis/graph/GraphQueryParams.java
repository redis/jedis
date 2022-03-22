package redis.clients.jedis.graph;

import java.util.HashMap;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.graph.GraphProtocol.GraphCommand;
import redis.clients.jedis.graph.GraphProtocol.GraphKeyword;
import redis.clients.jedis.params.IParams;

public class GraphQueryParams implements IParams {

  private boolean readonly;
  private String query;
  private Map<String, Object> params;
  private Long timeout;

  /**
   * Query string must be set later.
   */
  public GraphQueryParams() {
  }

  /**
   * Query string must be set later.
   */
  public static GraphQueryParams queryParams() {
    return new GraphQueryParams();
  }

  public GraphQueryParams(String query) {
    this.query = query;
  }

  public static GraphQueryParams queryParams(String query) {
    return new GraphQueryParams(query);
  }

  public GraphQueryParams readonly() {
    return readonly(true);
  }

  public GraphQueryParams readonly(boolean readonly) {
    this.readonly = readonly;
    return this;
  }

  public GraphQueryParams query(String queryStr) {
    this.query = queryStr;
    return this;
  }

  public GraphQueryParams params(Map<String, Object> params) {
    this.params = params;
    return this;
  }

  public GraphQueryParams addParam(String key, Object value) {
    if (this.params == null) this.params = new HashMap<>();
    this.params.put(key, value);
    return this;
  }

  public GraphQueryParams timeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (query == null) throw new JedisException("Query string must be set.");

    if (params == null) {
      args.add(query);
    } else {
      args.add(RedisGraphQueryUtil.prepareQuery(query, params));
    }

    args.add(GraphKeyword.__COMPACT);

    if (timeout != null) {
      args.add(GraphKeyword.TIMEOUT).add(timeout).blocking();
    }
  }

  public CommandArguments getArguments(String graphName) {
    return new CommandArguments(!readonly ? GraphCommand.QUERY : GraphCommand.RO_QUERY)
        .key(graphName).addParams(this);
  }
}
