package redis.clients.jedis.util.lock;

import java.util.concurrent.TimeUnit;

public interface JedisLock {
    void lock();

    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit);

    void unlock();

    void forceUnlock();
}
