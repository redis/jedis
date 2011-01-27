package com.googlecode.jedis;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Response for the {@link Jedis#type(String)} command.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public enum RedisType {
    STRING, LIST, SET, HASH, NONE;

    String type;

    private static final Map<String, RedisType> lookup;

    static {
	Map<String, RedisType> tmp = Maps.newHashMapWithExpectedSize(RedisType
		.values().length);
	for (RedisType it : RedisType.values()) {
	    tmp.put(it.type, it);
	}
	lookup = ImmutableMap.copyOf(tmp);
    }

    protected static RedisType get(byte[] type) {
	return lookup.get(new String(type, Protocol.DEFAULT_CHARSET));
    }

    protected static RedisType get(String type) {
	return lookup.get(type);
    }

    private RedisType() {
	this.type = this.name().toLowerCase();
    }

}
