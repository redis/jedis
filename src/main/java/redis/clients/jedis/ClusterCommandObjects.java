package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.KEYS;
import static redis.clients.jedis.Protocol.Command.SCAN;
import static redis.clients.jedis.Protocol.Keyword.TYPE;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.util.JedisClusterHashTag;

public class ClusterCommandObjects extends CommandObjects {

  @Override
  protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
    return new ClusterCommandArguments(command);
  }

  private static final String CLUSTER_UNSUPPORTED_MESSAGE = "Not supported in cluster mode.";

  @Override
  public CommandObject<Long> dbSize() {
    throw new UnsupportedOperationException(CLUSTER_UNSUPPORTED_MESSAGE);
  }

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
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).processKey(match), BuilderFactory.SCAN_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    String match = params.match();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).processKey(match).add(TYPE).add(type), BuilderFactory.SCAN_RESPONSE);
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
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).processKey(match), BuilderFactory.SCAN_BINARY_RESPONSE);
  }

  @Override
  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params, byte[] type) {
    byte[] match = params.binaryMatch();
    if (match == null || !JedisClusterHashTag.isClusterCompliantMatchPattern(match)) {
      throw new IllegalArgumentException(SCAN_PATTERN_MESSAGE);
    }
    return new CommandObject<>(commandArguments(SCAN).add(cursor).addParams(params).processKey(match).add(TYPE).add(type), BuilderFactory.SCAN_BINARY_RESPONSE);
  }

  @Override
  public final CommandObject<Long> waitReplicas(int replicas, long timeout) {
    throw new UnsupportedOperationException(CLUSTER_UNSUPPORTED_MESSAGE);
  }

  // RediSearch commands
  // TODO: Send RediSearch command to random 'master' node or random hashslot.
//  boolean searchLite = false;

  private <T> CommandObject<T> processSearchCommand(String indexName, CommandObject<T> command) {
//    if (searchLite) command.getArguments().processKey(indexName);
    command.getArguments().processKey(indexName);
    return command;
  }

  @Override
  public final CommandObject<String> ftCreate(String indexName, IndexOptions indexOptions, Schema schema) {
    return processSearchCommand(indexName, super.ftCreate(indexName, indexOptions, schema));
  }

  @Override
  public final CommandObject<String> ftAlter(String indexName, Schema schema) {
    return processSearchCommand(indexName, super.ftAlter(indexName, schema));
  }

  @Override
  public final CommandObject<SearchResult> ftSearch(String indexName, Query query) {
    return processSearchCommand(indexName, super.ftSearch(indexName, query));
  }

  @Override
  public final CommandObject<SearchResult> ftSearch(byte[] indexName, Query query) {
    CommandObject<SearchResult> command = super.ftSearch(indexName, query);
//    if (searchLite) command.getArguments().processKey(indexName);
    command.getArguments().processKey(indexName);
    return command;
  }

  @Override
  public CommandObject<String> ftExplain(String indexName, Query query) {
    return processSearchCommand(indexName, super.ftExplain(indexName, query));
  }

  @Override
  public CommandObject<List<String>> ftExplainCLI(String indexName, Query query) {
    return processSearchCommand(indexName, super.ftExplainCLI(indexName, query));
  }

  @Override
  public CommandObject<AggregationResult> ftAggregate(String indexName, AggregationBuilder aggr) {
    return processSearchCommand(indexName, super.ftAggregate(indexName, aggr));
  }

  @Override
  public CommandObject<AggregationResult> ftCursorRead(String indexName, long cursorId, int count) {
    return processSearchCommand(indexName, super.ftCursorRead(indexName, cursorId, count));
  }

  @Override
  public CommandObject<String> ftCursorDel(String indexName, long cursorId) {
    return processSearchCommand(indexName, super.ftCursorDel(indexName, cursorId));
  }

  @Override
  public CommandObject<String> ftDropIndex(String indexName) {
    return processSearchCommand(indexName, super.ftDropIndex(indexName));
  }

  @Override
  public CommandObject<String> ftDropIndexDD(String indexName) {
    return processSearchCommand(indexName, super.ftDropIndexDD(indexName));
  }

  @Override
  public CommandObject<String> ftSynUpdate(String indexName, String synonymGroupId, String... terms) {
    return processSearchCommand(indexName, super.ftSynUpdate(indexName, synonymGroupId, terms));
  }

  @Override
  public CommandObject<Map<String, List<String>>> ftSynDump(String indexName) {
    return processSearchCommand(indexName, super.ftSynDump(indexName));
  }

  @Override
  public CommandObject<Map<String, Object>> ftInfo(String indexName) {
    return processSearchCommand(indexName, super.ftInfo(indexName));
  }

  @Override
  public CommandObject<Set<String>> ftTagVals(String indexName, String fieldName) {
    return processSearchCommand(indexName, super.ftTagVals(indexName, fieldName));
  }

  @Override
  public CommandObject<String> ftAliasAdd(String aliasName, String indexName) {
//    CommandObject<String> command = super.ftAliasAdd(aliasName, indexName);
//    if (searchLite) command.getArguments().processKey(aliasName).processKey(indexName);
//    return command;
    return processSearchCommand(indexName, super.ftAliasAdd(aliasName, indexName));
  }

  @Override
  public CommandObject<String> ftAliasUpdate(String aliasName, String indexName) {
//    CommandObject<String> command = super.ftAliasUpdate(aliasName, indexName);
//    if (searchLite) command.getArguments().processKey(aliasName).processKey(indexName);
//    return command;
    return processSearchCommand(indexName, super.ftAliasUpdate(aliasName, indexName));
  }

  @Override
  public CommandObject<String> ftAliasDel(String aliasName) {
//    CommandObject<String> command = super.ftAliasDel(aliasName);
//    if (searchLite) command.getArguments().processKey(aliasName);
//    return command;
    return processSearchCommand(aliasName, super.ftAliasDel(aliasName));
  }

  @Override
  public CommandObject<Map<String, String>> ftConfigGet(String indexName, String option) {
    return processSearchCommand(indexName, super.ftConfigGet(indexName, option));
  }

  @Override
  public CommandObject<String> ftConfigSet(String indexName, String option, String value) {
    return processSearchCommand(indexName, super.ftConfigSet(indexName, option, value));
  }
  // RediSearch commands
}
