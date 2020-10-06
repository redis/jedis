/*
 * Copyright 2019-2119 gao_xianglong@sina.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package redis.clients.jedis.tests.utils.lock;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.util.lock.JedisLock;
import redis.clients.jedis.util.lock.JedisLockManager;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RedLockBenchmark {
    private static volatile JedisLock lock;

    @BeforeClass
    public static void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(10);
        config.setMaxIdle(50);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(2000);
        JedisLockManager manager = new JedisLockManager(Arrays.asList(new JedisPool(config, "127.0.0.1", 6379),
                new JedisPool(config, "127.0.0.1", 6380),
                new JedisPool(config, "127.0.0.1", 6381)));
        lock = manager.getLock("mylock");
    }

    @Test
    public void lockTest() {
        int threadSize = 10;
        int taskSize = 100000;
        CountDownLatch latch = new CountDownLatch(threadSize);
        long begin = System.nanoTime();
        for (int i = 0; i < threadSize; i++) {
            new Thread(() -> {
                for (int j = 0; j < taskSize / threadSize; j++) {
                    lock.lock();
                    lock.unlock();
                }
                latch.countDown();
            }).start();
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
        int threadSize = 20;
        int taskSize = 1000000;
        CountDownLatch latch = new CountDownLatch(threadSize);
        long begin = System.nanoTime();
        for (int i = 0; i < threadSize; i++) {
            new Thread(() -> {
                for (int j = 0; j < taskSize / threadSize; j++) {
                    if (lock.tryLock()) {
                        lock.unlock();
                    }
                }
                latch.countDown();
            }).start();
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
