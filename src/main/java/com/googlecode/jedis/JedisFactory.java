package com.googlecode.jedis;

import static com.googlecode.jedis.JedisConfig.newJedisConfig;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Factory for jedis instances.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public final class JedisFactory {

    /**
     * Get a {@link Jedis} instance with default settings.
     * 
     * @return a Jedis instance
     */
    static public Jedis newJedisInstance() {
	final Jedis jedis = new JedisImpl();
	jedis.setJedisConfig(newJedisConfig());
	try {
	    jedis.connect();
	} catch (final UnknownHostException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (final IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return jedis;

    }

    /**
     * Get a {@link Jedis} instance with specified settings.
     * 
     * @param config
     *            the config
     * @return a Jedis instance
     */
    static public Jedis newJedisInstance(final JedisConfig config) {
	final Jedis jedis = new JedisImpl();
	jedis.setJedisConfig(config);
	try {
	    jedis.connect();
	} catch (final UnknownHostException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (final IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return jedis;
    }

}
