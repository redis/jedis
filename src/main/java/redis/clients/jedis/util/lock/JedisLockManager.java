package redis.clients.jedis.util.lock;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.util.Pool;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JedisLockManager {
    private Pool pool;
    private JedisCluster jedisCluster;
    private boolean isCluster;
    private Map<String, JedisLock> lockMap = new ConcurrentHashMap<>(32);

    public JedisLockManager(Pool pool) {
        this.pool = pool;
    }

    public JedisLockManager(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        isCluster = true;
    }

    public JedisLock getLock(String name) {
        Objects.requireNonNull(name);
        JedisLock lock = null;
        synchronized (this) {
            lock = lockMap.get(name);
            if (null == lock) {
                LockCommand commands = null;
                if (isCluster) {
                    commands = new ClusterLockCommand(jedisCluster);
                } else {
                    commands = new NonClusterLockCommand(pool);
                }
                lock = new JedisReentrantLock(name, commands);
                lockMap.put(name, lock);
            }
        }
        return lock;
    }

    public Set<String> getLocks() {
        return lockMap.keySet();
    }
}
