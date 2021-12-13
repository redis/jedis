package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.KEYS;
import static redis.clients.jedis.Protocol.Command.SCAN;
import static redis.clients.jedis.Protocol.Keyword.TYPE;

import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.Hashing;
import redis.clients.jedis.util.JedisClusterHashTag;

public class ShardedCommandObjects extends CommandObjects {

  private final Hashing algo;
  private final Pattern tagPattern;

  public ShardedCommandObjects(Hashing algo) {
    this(algo, null);
  }

  public ShardedCommandObjects(Hashing algo, Pattern tagPattern) {
    this.algo = algo;
    this.tagPattern = tagPattern;
  }

  @Override
  protected ShardedCommandArguments commandArguments(ProtocolCommand command) {
    return new ShardedCommandArguments(algo, tagPattern, command);
  }

  @Override
  public CommandObject<Long> dbSize() {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
  }
}
