package redis.clients.jedis.util.lock;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.util.lock.redlock.JedisRedLock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JedisLockManager {
    private Pool pool;
    private JedisCluster jedisCluster;
    private List<Pool> pools = null;
    private LockType lockType;
    private Map<String, JedisLock> lockMap = new ConcurrentHashMap<>(32);

    public JedisLockManager(List<Pool> pools) {
        this.pools = pools;
        lockType = LockType.RED_LOCK;
    }

    public JedisLockManager(Pool pool) {
        this.pool = pool;
        lockType = LockType.SINGLE;
    }

    public JedisLockManager(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        lockType = LockType.CLUSTER;
    }

    enum LockType {
        RED_LOCK, CLUSTER, SINGLE;
    }

    public JedisLock getLock(String name) {
        Objects.requireNonNull(name);
        JedisLock result = null;
        synchronized (this) {
            result = lockMap.get(name);
            if (Objects.isNull(result)) {
                switch (lockType) {
                    case SINGLE:
                        result = new JedisReentrantLock(name, new NonClusterLockCommand(pool));
                        break;
                    case CLUSTER:
                        result = new JedisReentrantLock(name, new ClusterLockCommand(jedisCluster));
                        break;
                    case RED_LOCK:
                        List<JedisLock> locks = new ArrayList<>();
                        pools.forEach(pool -> locks.add(new JedisReentrantLock(name, new NonClusterLockCommand(pool))));
                        result = new JedisRedLock(locks);
                }
                lockMap.put(name, result);
            }
        }
        return result;
    }

    public Set<String> getLocks() {
        return lockMap.keySet();
    }
}
