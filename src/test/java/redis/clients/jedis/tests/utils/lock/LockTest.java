package redis.clients.jedis.tests.utils.lock;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.util.lock.JedisLock;
import redis.clients.jedis.util.lock.JedisLockManager;

import java.util.concurrent.TimeUnit;

public class LockTest {
    private volatile static JedisLock lock;

    @BeforeClass
    public static void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(10);
        config.setMaxIdle(50);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(1000);
        lock = new JedisLockManager(new JedisCluster(new HostAndPort("127.0.0.1", 6379),
                config)).getLock("mylock");
    }

    @Test
    public void lock() {
        try {
            lock.lock();
            System.out.println("Get lock success...");
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void tryLock() {
        try {
            Assert.assertTrue(lock.tryLock());
        } finally {
            lock.unlock();
        }
        try {
            Assert.assertTrue(lock.tryLock(1, TimeUnit.SECONDS));
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void forceUnlock() {
        try {
            lock.tryLock();
        } finally {
            lock.forceUnlock();//暴力释放锁
        }
    }
}
