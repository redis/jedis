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
    HASH, LIST, NONE, SET, STRING;

    private static final Map<String, RedisType> lookup;

    static {
	final Map<String, RedisType> tmp = Maps
		.newHashMapWithExpectedSize(RedisType.values().length);
	for (final RedisType it : RedisType.values()) {
	    tmp.put(it.type, it);
	}
	lookup = ImmutableMap.copyOf(tmp);
    }

    protected static RedisType get(final byte[] type) {
	return lookup.get(new String(type, Protocol.DEFAULT_CHARSET));
    }

    protected static RedisType get(final String type) {
	return lookup.get(type);
    }

    String type;

    private RedisType() {
	type = name().toLowerCase();
    }

}
