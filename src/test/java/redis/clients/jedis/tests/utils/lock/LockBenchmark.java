package redis.clients.jedis.tests.utils.lock;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.util.lock.JedisLock;
import redis.clients.jedis.util.lock.JedisLockManager;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LockBenchmark {
    private static volatile JedisLock lock;

    @BeforeClass
    public static void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(20);
        config.setMaxIdle(50);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(2000);
        lock = new JedisLockManager(new JedisCluster(new HostAndPort("127.0.0.1", 6379),
                config)).getLock("mylock");
    }

    @Test
    public void lockTest() {
        final int threadSize = 10;
        final int taskSize = 100000;
        final CountDownLatch latch = new CountDownLatch(threadSize);
        long begin = System.nanoTime();
        for (int i = 0; i < threadSize; i++) {
            new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < taskSize / threadSize; j++) {
                        lock.lock();
                        lock.unlock();
                    }
                    latch.countDown();
                }
            }.start();
        }
        try {
            latch.await();
            long end = System.nanoTime();
            long rt = TimeUnit.NANOSECONDS.toSeconds(end - begin);
            DecimalFormat df = new DecimalFormat("0.00");
            df.setRoundingMode(RoundingMode.HALF_UP);
            String avg = df.format((double) rt / taskSize);
            String tps = df.format((double) taskSize / rt);
            System.out.println(String.format("[threadSize]:%s, [taskSize]:%s, [rt]:%ss, [avg]:%ss, [tps]:%s/s", threadSize, taskSize, rt, avg,
                    tps));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void tryLock() {
        final int threadSize = 20;
        final int taskSize = 1000000;
        final CountDownLatch latch = new CountDownLatch(threadSize);
        long begin = System.nanoTime();
        for (int i = 0; i < threadSize; i++) {
            new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < taskSize / threadSize; j++) {
                        if (lock.tryLock()) {
                            lock.unlock();
                        }
                    }
                    latch.countDown();
                }
            }.start();
        }
        try {
            latch.await();
            long end = System.nanoTime();
            long rt = TimeUnit.NANOSECONDS.toSeconds(end - begin);
            DecimalFormat df = new DecimalFormat("0.00");
            df.setRoundingMode(RoundingMode.HALF_UP);
            String avg = df.format((double) rt / taskSize);
            String tps = df.format((double) taskSize / rt);
            System.out.println(String.format("[threadSize]:%s, [taskSize]:%s, [rt]:%ss, [avg]:%ss, [tps]:%s/s", threadSize, taskSize, rt, avg,
                    tps));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
