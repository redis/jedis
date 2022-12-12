package redis.clients.jedis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
      throw new NullPointerException("ConnectionProvider cannot be null.");
    }
    this.provider = provider;
  }

  public final <T> Map<?, Supplier<T>> broadcastCommand(CommandObject<T> commandObject) {
    Map<?, ?> connectionMap = provider.getConnectionMap();
    Map<Object, Supplier<T>> responseMap = new HashMap<>(connectionMap.size(), 1f);
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
    } else if (connection instanceof Pool) {
      try (Connection _conn = ((Pool<Connection>) connection).getResource()) {
        return _conn.executeCommand(commandObject);
      }
    }
    throw new IllegalStateException(connection.getClass() + "is not supported.");
  }

  public Map<?, Supplier<String>> ping() {
    return broadcastCommand(new CommandObject<>(new CommandArguments(Command.PING),
        BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> configSet(final String... parameterValues) {
    if (parameterValues.length > 0 && parameterValues.length % 2 != 0) {
      throw new IllegalArgumentException("It requires 'pair's of config parameter-values.");
    }
    CommandArguments args = new CommandArguments(Command.CONFIG).add(Keyword.SET)
        .addObjects((Object[]) parameterValues);
    return broadcastCommand(new CommandObject<>(args, BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> flushDB() {
    return broadcastCommand(new CommandObject<>(new CommandArguments(Command.FLUSHDB),
        BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> flushDB(FlushMode flushMode) {
    return broadcastCommand(new CommandObject<>(new CommandArguments(Command.FLUSHDB)
        .add(flushMode), BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> flushAll() {
    return broadcastCommand(new CommandObject<>(new CommandArguments(Command.FLUSHALL),
        BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> flushAll(FlushMode flushMode) {
    return broadcastCommand(new CommandObject<>(new CommandArguments(Command.FLUSHALL)
        .add(flushMode), BuilderFactory.STRING));
  }

  public Map<?, Supplier<Long>> waitReplicas(final int replicas, final long timeout) {
    CommandArguments args = new CommandArguments(Command.WAIT)
        .add(Protocol.toByteArray(replicas)).add(Protocol.toByteArray(timeout));
    return broadcastCommand(new CommandObject<>(args, BuilderFactory.LONG));
  }

  public Map<?, Supplier<List<Boolean>>> scriptExists(String... sha1) {
    CommandObject<List<Boolean>> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.EXISTS).addObjects((Object[]) sha1), BuilderFactory.BOOLEAN_LIST);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<List<Boolean>>> scriptExists(byte[]... sha1) {
    CommandObject<List<Boolean>> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.EXISTS).addObjects((Object[]) sha1), BuilderFactory.BOOLEAN_LIST);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<String>> scriptLoad(String script) {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.LOAD).add(script), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<byte[]>> scriptLoad(byte[] script) {
    CommandObject<byte[]> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.LOAD).add(script), BuilderFactory.BINARY);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<String>> scriptFlush() {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.FLUSH), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<String>> scriptFlush(FlushMode flushMode) {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.FLUSH).add(flushMode), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<String>> scriptKill() {
    CommandObject<String> command = new CommandObject<>(new CommandArguments(Command.SCRIPT)
        .add(Keyword.KILL), BuilderFactory.STRING);
    return broadcastCommand(command);
  }

  public Map<?, Supplier<String>> ftCreate(String indexName, IndexOptions indexOptions, Schema schema) {
    CommandArguments args = new CommandArguments(SearchCommand.CREATE).add(indexName)
        .addParams(indexOptions).add(SearchKeyword.SCHEMA);
    schema.fields.forEach(field -> args.addParams(field));
    return broadcastCommand(new CommandObject<>(args, BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> ftCreate(String indexName, SchemaField... schemaFields) {
    return ftCreate(indexName, Arrays.asList(schemaFields));
  }

  public Map<?, Supplier<String>> ftCreate(String indexName, FTCreateParams createParams, SchemaField... schemaFields) {
    return ftCreate(indexName, createParams, Arrays.asList(schemaFields));
  }

  public Map<?, Supplier<String>> ftCreate(String indexName, Iterable<SchemaField> schemaFields) {
    return ftCreate(indexName, FTCreateParams.createParams(), schemaFields);
  }

  public Map<?, Supplier<String>> ftCreate(String indexName, FTCreateParams createParams,
      Iterable<SchemaField> schemaFields) {
    CommandArguments args = new CommandArguments(SearchCommand.CREATE).add(indexName)
        .addParams(createParams).add(SearchKeyword.SCHEMA);
    schemaFields.forEach(field -> args.addParams(field));
    return broadcastCommand(new CommandObject<>(args, BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> ftDropIndex(String indexName) {
    return broadcastCommand(new CommandObject<>(new CommandArguments(SearchCommand.DROPINDEX)
        .add(indexName), BuilderFactory.STRING));
  }

  public Map<?, Supplier<String>> ftDropIndexDD(String indexName) {
    return broadcastCommand(new CommandObject<>(new CommandArguments(SearchCommand.DROPINDEX)
        .add(indexName).add(SearchKeyword.DD), BuilderFactory.STRING));
  }
}
