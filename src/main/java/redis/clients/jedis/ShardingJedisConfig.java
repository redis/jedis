package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

/**
 * Config builder for a sharded Redis.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public class ShardingJedisConfig {

    // the tag is anything between {}
    // TODO: use string pattern in config
    private static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern
	    .compile("\\{(.+?)\\}");

    private static final HashingStrategy DEFAULT_HASHING_STRATEGY = new MurmurHashingStrategy();

    static public ShardingJedisConfig newShardingJedisConfig() {
	return new ShardingJedisConfig();
    }

    private List<ShardJedisConfig> shardConfigs;

    private Pattern keyTagPattern;

    private HashingStrategy hashingStrategy;

    private ShardingJedisConfig() {
	this.hashingStrategy = DEFAULT_HASHING_STRATEGY;
	this.keyTagPattern = DEFAULT_KEY_TAG_PATTERN;
	this.shardConfigs = Lists.newArrayList();
    }

    /**
     * Add one shard config.
     * 
     * @param shard
     * @return the ShardingJedisConfig instance
     */
    public ShardingJedisConfig addShard(ShardJedisConfig shard) {
	shardConfigs.add(shard);
	return this;
    }

    /**
     * Get the hashing strategy
     * 
     * @return the hashing strategy
     */
    public HashingStrategy getHashingStrategy() {
	return hashingStrategy;
    }

    /**
     * Get the keytag pattern.
     * 
     * @return the keytag pattern
     */
    public Pattern getKeyTagPattern() {
	return keyTagPattern;
    }

    /**
     * Get the shard configs.
     * 
     * @return the shard configs
     */
    public List<ShardJedisConfig> getShardConfigs() {
	return shardConfigs;
    }

    /**
     * Set the hashing strategy.
     * 
     * @param hashingStrategy
     * @return the ShardingJedisConfig instance.
     */
    public ShardingJedisConfig hashingStrategy(HashingStrategy hashingStrategy) {
	this.hashingStrategy = hashingStrategy;
	return this;
    }

    /**
     * Set the pattern used for extracting a key tag.
     * 
     * The pattern must have a group (between parenthesis), which delimits the
     * tag to be hashed. A null pattern avoids applying the regular expression
     * for each lookup, improving performance a little bit is key tags aren't
     * being used.
     * 
     * Default is "{}";
     * 
     * @param tagPattern
     *            the pattern
     * @return the ShardingJedisConfig instance
     */
    public ShardingJedisConfig keyTagPattern(Pattern keyTagPattern) {
	this.keyTagPattern = keyTagPattern;
	return this;
    }

    /**
     * Set the hashing strategy.
     * 
     * @param hashingStrategy
     */
    public void setHashingStrategy(HashingStrategy hashingStrategy) {
	this.hashingStrategy = hashingStrategy;
    }

    /**
     * Set the pattern used for extracting a key tag.
     * 
     * The pattern must have a group (between parenthesis), which delimits the
     * tag to be hashed. A null pattern avoids applying the regular expression
     * for each lookup, improving performance a little bit is key tags aren't
     * being used.
     * 
     * Default is "{}";
     * 
     * @param tagPattern
     *            the pattern
     */
    public void setKeyTagPattern(Pattern keyTagPattern) {
	this.keyTagPattern = keyTagPattern;
    }

    /**
     * Set the shard configs.
     * 
     * @param shardConfigs
     */
    public void setShardConfigs(List<ShardJedisConfig> shardConfigs) {
	this.shardConfigs = shardConfigs;
    }

}
