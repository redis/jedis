package redis.clients.jedis;

import java.util.function.Consumer;
import java.util.function.Function;


public interface JedisResourceProvider {

    Jedis getResource();

    default void withResource(Consumer<Jedis> consumer) {
        try (Jedis jedis = this.getResource()) {
            consumer.accept(jedis);
        }
    }

    default <K> K withResourceGet(Function<Jedis, K> function) {
        try (Jedis jedis = this.getResource()) {
            return function.apply(jedis);
        }
    }
}
