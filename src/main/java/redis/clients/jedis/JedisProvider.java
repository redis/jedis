package redis.clients.jedis;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface for providing Jedis resources from a pool
 */
public interface JedisProvider {

    /**
     * Get a Jedis resource from the pool
     * @return Jedis object
     */
    Jedis getResource();

    /**
     * Return a Jedis resource to the pool
     * @param resource Jedis object
     */
    void returnResource(final Jedis resource);

    /**
     * Execute a consumer with a Jedis resource
     * @param consumer code to execute with the Jedis resource
     */
    default void withResource(Consumer<Jedis> consumer) {
        try (Jedis jedis = this.getResource()) {
            consumer.accept(jedis);
        }
    }

    /**
     * Execute a function with a Jedis resource and return a value
     * @param function code to execute with the Jedis resource
     * @return return value from the function
     */
    default <K> K withResourceGet(Function<Jedis, K> function) {
        try (Jedis jedis = this.getResource()) {
            return function.apply(jedis);
        }
    }

}
