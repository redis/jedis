package redis.clients.jedis;

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
     * @param config
     * @return a Jedis instance
     */
    static public Jedis newJedisInstance() {
	return new JedisImpl();

    }

    /**
     * Get a {@link Jedis} instance with specified settings.
     * 
     * @param config
     *            the config
     * @return a Jedis instance
     */
    static public Jedis newJedisInstance(JedisConfig config) {
	Jedis jedis = new JedisImpl();
	jedis.setJedisConfig(config);
	return jedis;
    }

}
