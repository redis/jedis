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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.util.lock.JedisLock;
import redis.clients.jedis.util.lock.JedisLockManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/8/18 10:19 上午
 */
public class RedLockTest {
    private static JedisLock lock;

    @BeforeClass
    public static void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(10);
        config.setMaxIdle(50);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(1000);
        JedisLockManager manager = new JedisLockManager(Arrays.asList(new JedisPool(config, "127.0.0.1", 6379),
                new JedisPool(config, "127.0.0.1", 6380),
                new JedisPool(config, "127.0.0.1", 6381)));
        lock = manager.getLock("mylock");
    }

    @Test
    public void lock() {
        try {
            //同步获取重入锁，当前线程如果获取锁资源失败则一直阻塞直至成功
            lock.lock();
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void tryLock() {
        try {
            System.out.println(lock.tryLock());
        } finally {
            lock.unlock();
        }
        try {
            Assert.assertTrue(lock.tryLock(10, TimeUnit.SECONDS));
        } finally {
            lock.unlock();
        }
    }
}
