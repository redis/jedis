package redis.clients.jedis.util.lock;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ClusterLockCommand implements LockCommand {
    private JedisCluster jedisCluster;

    protected ClusterLockCommand(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        return jedisCluster.eval(script, keyCount, params);
    }

    @Override
    public String scriptLoad(String script) {
        return jedisCluster.scriptLoad(script, script);
    }

    @Override
    public Object evalsha(String script, int keyCount, String... params) {
        return jedisCluster.evalsha(script, keyCount, params);
    }

    @Override
    public void subscribe(Future<?> future, JedisPubSub jedisPubSub, String... channels) {
        while (true) {
            try {
                jedisCluster.subscribe(jedisPubSub, channels);
            } catch (Throwable e) {
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
