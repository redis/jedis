package com.googlecode.jedis;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * The response types of redis.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 */
enum ResponseType {
    Bulk('$'), Error('-'), Integer(':'), MultiBulk('*'), SingleLine('+'), Unknown(
	    '?');

    private static final ImmutableMap<Byte, ResponseType> lookup;

    // init the lookup map
    static {
	final Map<Byte, ResponseType> tmp = Maps
		.newHashMapWithExpectedSize(ResponseType.values().length);
	for (final ResponseType it : ResponseType.values()) {
	    tmp.put((byte) it.prefix, it);
	}
	lookup = ImmutableMap.copyOf(tmp);
    }

    static ResponseType get(final byte prefix) {
	ResponseType responseType = lookup.get(prefix);

	if (responseType == null) {
	    responseType = Unknown;
	}

	return responseType;
    }

    private char prefix;

    private ResponseType(final char prefix) {
	this.prefix = prefix;
    }
}
