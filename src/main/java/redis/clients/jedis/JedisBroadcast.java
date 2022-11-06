package redis.clients.jedis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.util.Pool;

public class JedisBroadcast {

  private final ConnectionProvider provider;

  public JedisBroadcast(UnifiedJedis jedis) {
    this(jedis.provider);
  }

  public JedisBroadcast(ConnectionProvider provider) {
    if (provider == null) {
      throw new NullPointerException("ConnectionProvider is null.");
    }
    this.provider = provider;
  }

  public final <T> Map<?, BroadcastResponse<T>> broadcastCommand(CommandObject<T> commandObject) {
    Map<?, ?> connectionMap = provider.getConnectionMap();
    Map<Object, BroadcastResponse<T>> responseMap = new HashMap<>(connectionMap.size(), 1f);
    for (Map.Entry<? extends Object, ? extends Object> entry : connectionMap.entrySet()) {
      Object key = entry.getKey();
      Object connection = entry.getValue();
      try {
        responseMap.put(key, new BroadcastResponse<>(executeCommand(connection, commandObject)));
      } catch (RuntimeException re) {
        responseMap.put(key, new BroadcastResponse<>(re));
      }
    }
    return responseMap;
  }

  private <T> T executeCommand(Object connection, CommandObject<T> commandObject) {
    if (connection instanceof Connection) {
      return ((Connection) connection).executeCommand(commandObject);
    }
    if (connection instanceof Pool) {
      try (Connection _conn = ((Pool<Connection>) connection).getResource()) {
        return _conn.executeCommand(commandObject);
      }
    }
    throw new IllegalStateException(connection.getClass() + "is not supported.");
  }

  public Map<?, BroadcastResponse<List<Boolean>>> scriptExists(String... sha1) {
    CommandObject<List<Boolean>> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.EXISTS).addObjects((Object[]) sha1), BuilderFactory.BOOLEAN_LIST);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<List<Boolean>>> scriptExists(byte[]... sha1) {
    CommandObject<List<Boolean>> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.EXISTS).addObjects((Object[]) sha1), BuilderFactory.BOOLEAN_LIST);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<String>> scriptLoad(String script) {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.LOAD).add(script), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<byte[]>> scriptLoad(byte[] script) {
    CommandObject<byte[]> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.LOAD).add(script), BuilderFactory.BINARY);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<String>> scriptFlush() {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.FLUSH), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<String>> scriptFlush(FlushMode flushMode) {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.FLUSH).add(flushMode), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<String>> scriptKill() {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.KILL), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, BroadcastResponse<String>> ftCreate(String indexName, IndexOptions indexOptions, Schema schema) {
    CommandArguments args = new CommandArguments(SearchCommand.CREATE).add(indexName)
        .addParams(indexOptions).add(SearchKeyword.SCHEMA);
    schema.fields.forEach(field -> args.addParams(field));
    return broadcastCommand(new CommandObject<>(args, BuilderFactory.STRING));
  }

  public Map<?, BroadcastResponse<String>> ftCreate(String indexName, SchemaField... schemaFields) {
    return ftCreate(indexName, Arrays.asList(schemaFields));
  }

  public Map<?, BroadcastResponse<String>> ftCreate(String indexName, FTCreateParams createParams, SchemaField... schemaFields) {
    return ftCreate(indexName, createParams, Arrays.asList(schemaFields));
  }

  public Map<?, BroadcastResponse<String>> ftCreate(String indexName, Iterable<SchemaField> schemaFields) {
    return ftCreate(indexName, FTCreateParams.createParams(), schemaFields);
  }

  public Map<?, BroadcastResponse<String>> ftCreate(String indexName, FTCreateParams createParams,
      Iterable<SchemaField> schemaFields) {
    CommandArguments args = new CommandArguments(SearchCommand.CREATE).add(indexName)
        .addParams(createParams).add(SearchKeyword.SCHEMA);
    schemaFields.forEach(field -> args.addParams(field));
    return broadcastCommand(new CommandObject<>(args, BuilderFactory.STRING));
  }
}
