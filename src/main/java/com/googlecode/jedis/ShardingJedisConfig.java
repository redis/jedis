package com.googlecode.jedis;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

/**
 * Config builder for a sharded Redis.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
class ShardingJedisConfig {

    private static final HashingStrategy DEFAULT_HASHING_STRATEGY = new MurmurHashingStrategy();

    // the tag is anything between {}
    // TODO: use string pattern in config
    private static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern
	    .compile("\\{(.+?)\\}");

    static public ShardingJedisConfig newShardingJedisConfig() {
	return new ShardingJedisConfig();
    }

    private HashingStrategy hashingStrategy;

    private Pattern keyTagPattern;

    private List<ShardJedisConfig> shardConfigs;

    private ShardingJedisConfig() {
	hashingStrategy = DEFAULT_HASHING_STRATEGY;
	keyTagPattern = DEFAULT_KEY_TAG_PATTERN;
	shardConfigs = Lists.newArrayList();
    }

    /**
     * Add one shard config.
     * 
     * @param shard
     * @return the ShardingJedisConfig instance
     */
    public ShardingJedisConfig addShard(final ShardJedisConfig shard) {
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
    public ShardingJedisConfig hashingStrategy(
	    final HashingStrategy hashingStrategy) {
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
     * @param keyTagPattern
     *            the pattern
     * @return the ShardingJedisConfig instance
     */
    public ShardingJedisConfig keyTagPattern(final Pattern keyTagPattern) {
	this.keyTagPattern = keyTagPattern;
	return this;
    }

    /**
     * Set the hashing strategy.
     * 
     * @param hashingStrategy
     */
    public void setHashingStrategy(final HashingStrategy hashingStrategy) {
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
     * @param keyTagPattern
     *            the pattern
     */
    public void setKeyTagPattern(final Pattern keyTagPattern) {
	this.keyTagPattern = keyTagPattern;
    }

    /**
     * Set the shard configs.
     * 
     * @param shardConfigs
     */
    public void setShardConfigs(final List<ShardJedisConfig> shardConfigs) {
	this.shardConfigs = shardConfigs;
    }

}
