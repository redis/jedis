package redis.clients.jedis.util.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.Pool;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NonClusterLockCommand implements LockCommand {
    private Pool pool;

    protected NonClusterLockCommand(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        Jedis jedis = null;
        try {
            jedis = (Jedis) pool.getResource();
            if (null != jedis) {
                return jedis.eval(script, keyCount, params);
            }
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public String scriptLoad(String script) {
        Jedis jedis = null;
        try {
            jedis = (Jedis) pool.getResource();
            if (null != jedis) {
                return jedis.scriptLoad(script);
            }
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public Object evalsha(String script, int keyCount, String... params) {
        Jedis jedis = null;
        try {
            jedis = (Jedis) pool.getResource();
            if (null != jedis) {
                return jedis.evalsha(script, keyCount, params);
            }
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public void subscribe(Future<?> future, JedisPubSub jedisPubSub, String... channels) {
        while (true) {
            try {
                Jedis jedis = null;
                try {
                    jedis = (Jedis) pool.getResource();
                    if (null != jedis) {
                        jedis.subscribe(jedisPubSub, channels);
                    }
                } finally {
                    if (null != jedis) {
                        jedis.close();
                    }
                }
            } catch (JedisConnectionException e) {
                if (null != future) {
                    future.cancel(true);
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException interruptedException) {
                    //...
                }
            }
        }
    }
}
