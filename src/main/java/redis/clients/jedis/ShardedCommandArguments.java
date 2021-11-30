package redis.clients.jedis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Hashing;

public class ShardedCommandArguments extends CommandArguments {

  private final Hashing algo;
  private final Pattern tagPattern;
  private Long keyHash = null;

  public ShardedCommandArguments(Hashing algo, ProtocolCommand command) {
    this(algo, null, command);
  }

  public ShardedCommandArguments(Hashing algo, Pattern tagPattern, ProtocolCommand command) {
    super(command);
    this.algo = algo;
    this.tagPattern = tagPattern;
  }

  public Long getKeyHash() {
    return keyHash;
  }

  @Override
  protected CommandArguments processKey(byte[] key) {
    final long hash = algo.hash(key);
    if (keyHash == null) {
      keyHash = hash;
    } else if (keyHash != hash) {
      throw new JedisException("Keys must generate same hash.");
    }
    return this;
  }

  @Override
  protected CommandArguments processKey(String key) {
    key = getKeyTag(key);
    final long hash = algo.hash(key);
    if (keyHash == null) {
      keyHash = hash;
    } else if (keyHash != hash) {
      throw new JedisException("Keys must generate same hash.");
    }
    return this;
  }

  private String getKeyTag(String key) {
    if (tagPattern != null) {
      Matcher m = tagPattern.matcher(key);
      if (m.find()) return m.group(1);
    }
    return key;
  }
}
