package com.googlecode.jedis;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;

import com.google.common.collect.Maps;

public class Sharded {

    public static final int DEFAULT_WEIGHT = 1;

    private TreeMap<Long, ShardJedisConfig> shardConfigs;

    private final Map<ShardJedisConfig, Jedis> resources = Maps.newHashMap();

    ShardingJedisConfig shardingConfig;

    protected Sharded(ShardingJedisConfig shardingConfig) {
	this.shardingConfig = shardingConfig;
	init();
    }

    public Collection<ShardJedisConfig> getAllShardInfo() {
	return Collections.unmodifiableCollection(shardConfigs.values());
    }

    public Collection<Jedis> getAllShards() {
	return Collections.unmodifiableCollection(resources.values());
    }

    /**
     * A key tag is a special pattern inside a key that, if preset, is the only
     * part of the key hashed in order to select the server for this key.
     * 
     * @param key
     * @return The tag if it exists, or the original key
     */
    public String getKeyTag(String key) {
	if (shardingConfig.getKeyTagPattern() != null) {
	    Matcher m = shardingConfig.getKeyTagPattern().matcher(key);
	    if (m.find()) {
		return m.group(1);
	    }
	}
	return key;
    }

    public Jedis getShard(byte[] key) {
	return resources.get(getShardInfo(key));
    }

    public Jedis getShard(String key) {
	return resources.get(getShardInfo(key));
    }

    private ShardJedisConfig getShardInfo(byte[] key) {
	SortedMap<Long, ShardJedisConfig> tail = shardConfigs
		.tailMap(shardingConfig.getHashingStrategy().hash64(key));
	if (tail.size() == 0) {
	    return shardConfigs.get(shardConfigs.firstKey());
	}
	return tail.get(tail.firstKey());
    }

    public ShardJedisConfig getShardInfo(String key) {
	return getShardInfo(SafeEncoder.encode(getKeyTag(key)));
    }

    private void init() {
	shardConfigs = Maps.newTreeMap();

	for (ShardJedisConfig shardConfig : shardingConfig.getShardConfigs()) {
	    for (int n = 0; n < 160 * shardConfig.getWeight(); n++) {
		shardConfigs.put(
			shardingConfig.getHashingStrategy()
				.hash64(String.format("%s%s",
					shardConfig.toString(), n)),
			shardConfig);
	    }
	    Jedis jedis = JedisFactory.newJedisInstance(shardConfig);
	    resources.put(shardConfig, jedis);
	}
    }
}