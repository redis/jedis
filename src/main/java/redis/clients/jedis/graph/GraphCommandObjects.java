package redis.clients.jedis.graph;

import static redis.clients.jedis.BuilderFactory.STRING;
import static redis.clients.jedis.graph.GraphProtocol.GraphKeyword.__COMPACT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import redis.clients.jedis.Builder;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.graph.GraphProtocol.GraphCommand;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * @deprecated Redis Graph support is deprecated.
 */
@Deprecated
public class GraphCommandObjects {

  private final RedisGraphCommands graph;
  private final Connection connection;
  private final ConnectionProvider provider;
  private Function<ProtocolCommand, CommandArguments> commArgs = (comm) -> new CommandArguments(comm);

  private final ConcurrentHashMap<String, Builder<ResultSet>> builders = new ConcurrentHashMap<>();

  public GraphCommandObjects(RedisGraphCommands graphCommands) {
    this.graph = graphCommands;
    this.connection = null;
    this.provider = null;
  }

  public GraphCommandObjects(Connection connection) {
    this.graph = null;
    this.connection = connection;
    this.provider = null;
  }

  public GraphCommandObjects(ConnectionProvider provider) {
    this.graph = null;
    this.connection = null;
    this.provider = provider;
  }

  public void setBaseCommandArgumentsCreator(Function<ProtocolCommand, CommandArguments> commArgs) {
    this.commArgs = commArgs;
  }

  // RedisGraph commands
  public final CommandObject<ResultSet> graphQuery(String name, String query) {
    return new CommandObject<>(commArgs.apply(GraphCommand.QUERY).key(name).add(query).add(__COMPACT), getBuilder(name));
  }

  public final CommandObject<ResultSet> graphReadonlyQuery(String name, String query) {
    return new CommandObject<>(commArgs.apply(GraphCommand.RO_QUERY).key(name).add(query).add(__COMPACT), getBuilder(name));
  }

  public final CommandObject<ResultSet> graphQuery(String name, String query, long timeout) {
    return graphQuery(name, GraphQueryParams.queryParams(query).timeout(timeout));
  }

  public final CommandObject<ResultSet> graphReadonlyQuery(String name, String query, long timeout) {
    return graphQuery(name, GraphQueryParams.queryParams().readonly().query(query).timeout(timeout));
  }

  public final CommandObject<ResultSet> graphQuery(String name, String query, Map<String, Object> params) {
    return graphQuery(name, GraphQueryParams.queryParams(query).params(params));
  }

  public final CommandObject<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params) {
    return graphQuery(name, GraphQueryParams.queryParams().readonly().query(query).params(params));
  }

  public final CommandObject<ResultSet> graphQuery(String name, String query, Map<String, Object> params, long timeout) {
    return graphQuery(name, GraphQueryParams.queryParams(query).params(params).timeout(timeout));
  }

  public final CommandObject<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout) {
    return graphQuery(name, GraphQueryParams.queryParams().readonly().query(query).params(params).timeout(timeout));
  }

  private CommandObject<ResultSet> graphQuery(String name, GraphQueryParams params) {
    return new CommandObject<>(
        commArgs.apply(!params.isReadonly() ? GraphCommand.QUERY : GraphCommand.RO_QUERY)
            .key(name).addParams(params), getBuilder(name));
  }

  public final CommandObject<String> graphDelete(String name) {
    return new CommandObject<>(commArgs.apply(GraphCommand.DELETE).key(name), STRING);
  }
  // RedisGraph commands

  private Builder<ResultSet> getBuilder(String graphName) {
    if (!builders.containsKey(graphName)) {
      createBuilder(graphName);
    }
    return builders.get(graphName);
  }

  private void createBuilder(String graphName) {
    builders.computeIfAbsent(graphName, graphNameKey -> new ResultSetBuilder(new GraphCacheImpl(graphNameKey)));
  }

  private class GraphCacheImpl implements GraphCache {

    private final GraphCacheList labels;
    private final GraphCacheList propertyNames;
    private final GraphCacheList relationshipTypes;

    public GraphCacheImpl(String graphName) {
      this.labels = new GraphCacheList(graphName, "db.labels");
      this.propertyNames = new GraphCacheList(graphName, "db.propertyKeys");
      this.relationshipTypes = new GraphCacheList(graphName, "db.relationshipTypes");
    }

    @Override
    public String getLabel(int index) {
      return labels.getCachedData(index);
    }

    @Override
    public String getRelationshipType(int index) {
      return relationshipTypes.getCachedData(index);
    }

    @Override
    public String getPropertyName(int index) {
      return propertyNames.getCachedData(index);
    }
  }

  private class GraphCacheList {

    private final String name;
    private final String query;
    private final List<String> data = new CopyOnWriteArrayList<>();
    
    private final Lock dataLock = new ReentrantLock(true);

    /**
     *
     * @param name - graph id
     * @param procedure - exact procedure command
     */
    public GraphCacheList(String name, String procedure) {
      this.name = name;
      this.query = "CALL " + procedure + "()";
    }

    /**
     * A method to return a cached item if it is in the cache, or re-validate the cache if its
     * invalidated
     *
     * @param index index of data item
     * @return The string value of the specific procedure response, at the given index.
     */
    public String getCachedData(int index) {
      if (index >= data.size()) {
        dataLock.lock();
        
        try {
          if (index >= data.size()) {
            getProcedureInfo();
          }
        } finally {
          dataLock.unlock();
        }
      }
      
      return data.get(index);
    }

    /**
     * Auxiliary method to parse a procedure result set and refresh the cache
     */
    private void getProcedureInfo() {
      ResultSet resultSet = callProcedure();
      Iterator<Record> it = resultSet.iterator();
      List<String> newData = new ArrayList<>();
      int i = 0;
      while (it.hasNext()) {
        Record record = it.next();
        if (i >= data.size()) {
          newData.add(record.getString(0));
        }
        i++;
      }
      data.addAll(newData);
    }

    private ResultSet callProcedure() {

      if (graph != null) {
        return graph.graphQuery(name, query);
      }

      CommandObject<ResultSet> commandObject = new CommandObject(
          new CommandArguments(GraphProtocol.GraphCommand.QUERY).key(name).add(query)
              .add(GraphProtocol.GraphKeyword.__COMPACT),
          getBuilder(name));

      if (connection != null) {
        return connection.executeCommand(commandObject);
      } else {
        try (Connection provided = provider.getConnection(commandObject.getArguments())) {
          return provided.executeCommand(commandObject);
        }
      }
    }
  }
}
