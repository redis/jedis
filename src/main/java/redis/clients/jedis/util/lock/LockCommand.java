package redis.clients.jedis.util.lock;

import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.Future;

public interface LockCommand {
    Object eval(String script, int keyCount, String... params);

    String scriptLoad(String script);

    Object evalsha(String script, int keyCount, String... params);

    void subscribe(Future<?> future, JedisPubSub jedisPubSub, String... channels);
}
