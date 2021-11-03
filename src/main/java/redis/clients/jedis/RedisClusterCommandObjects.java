package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.KEYS;
import static redis.clients.jedis.Protocol.Command.SCAN;
import static redis.clients.jedis.Protocol.Command.WAIT;
import static redis.clients.jedis.Protocol.Keyword.TYPE;

import java.util.Set;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.util.JedisClusterHashTag;

public class RedisClusterCommandObjects extends RedisCommandObjects {

  @Override
  protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
    return new ClusterCommandArguments(command);
  }

  private static final String CLUSTER_UNSUPPORTED_MESSAGE = "Not supported in cluster mode.";

  private static final String KEYS_PATTERN_MESSAGE = "Cluster mode only supports KEYS command"
      + " with pattern containing hash-tag ( curly-brackets enclosed string )";

  private static final String SCAN_PATTERN_MESSAGE = "Cluster mode only supports SCAN command"
      + " with MATCH pattern containing hash-tag ( curly-brackets enclosed string )";

  @Override
  public final CommandObject<Set<String>> keys(String pattern) {
    if (!JedisClusterHashTag.isClusterCompliantMatchPattern(pattern)) {
      throw new IllegalArgumentException(KEYS_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(KEYS).key(pattern).processKey(pattern), BuilderFactory.STRING_SET);
  }

  @Override
  public final CommandObject<Set<byte[]>> keys(byte[] pattern) {
    if (!JedisClusterHashTag.isClusterCompliantMatchPattern(pattern)) {
      throw new IllegalArgumentException(KEYS_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(KEYS).key(pattern).processKey(pattern), BuilderFactory.BINARY_SET);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor) {
    throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params) {
    String match = params.match();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).addParams(params).processKey(match), BuilderFactory.SCAN_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    String match = params.match();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).addParams(params).processKey(match).add(TYPE).add(type), BuilderFactory.SCAN_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor) {
    throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params) {
    byte[] match = params.binaryMatch();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).addParams(params).processKey(match), BuilderFactory.SCAN_BINARY_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params, byte[] type) {
    byte[] match = params.binaryMatch();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).addParams(params).processKey(match).add(TYPE).add(type), BuilderFactory.SCAN_BINARY_RESPONSE);
  }

  @Override
  public final CommandObject<Long> waitReplicas(int replicas, long timeout) {
    throw new UnsupportedOperationException(CLUSTER_UNSUPPORTED_MESSAGE);
  }

  boolean searchLite = false;

  private <T> CommandObject<T> processSearchCommand(String indexName, CommandObject<T> command) {
    if (searchLite) command.getArguments().processKey(indexName);
    return command;
  }

  private <T> CommandObject<T> processSearchCommand(byte[] indexName, CommandObject<T> command) {
    if (searchLite) command.getArguments().processKey(indexName);
    return command;
  }

  @Override
  public CommandObject<String> ftCreate(String indexName, IndexOptions indexOptions, Schema schema) {
    return processSearchCommand(indexName, super.ftCreate(indexName, indexOptions, schema));
  }

  @Override
  public CommandObject<SearchResult> ftSearch(String indexName, Query query) {
    return processSearchCommand(indexName, super.ftSearch(indexName, query));
  }

  @Override
  public CommandObject<SearchResult> ftSearch(byte[] indexName, Query query) {
    return processSearchCommand(indexName, super.ftSearch(indexName, query));
  }
}
