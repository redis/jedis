package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.KEYS;
import static redis.clients.jedis.Protocol.Command.SCAN;
import static redis.clients.jedis.Protocol.Keyword.TYPE;

import java.util.Set;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.JedisClusterHashTag;
import redis.clients.jedis.util.KeyValue;

public class ClusterCommandObjects extends CommandObjects {

  @Override
  protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
    ClusterCommandArguments comArgs = new ClusterCommandArguments(command);
    if (keyPreProcessor != null) comArgs.setKeyArgumentPreProcessor(keyPreProcessor);
    return comArgs;
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

  @Override
  public CommandObject<KeyValue<Long, Long>> waitAOF(long numLocal, long numReplicas, long timeout) {
    throw new UnsupportedOperationException(CLUSTER_UNSUPPORTED_MESSAGE);
  }

}
