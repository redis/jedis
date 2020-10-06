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
package redis.clients.jedis.util.lock.redlock;

import redis.clients.jedis.exceptions.JedisLockException;
import redis.clients.jedis.util.lock.JedisLock;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class JedisMultiLock implements JedisLock {
    protected List<JedisLock> locks;

    protected JedisMultiLock(List<JedisLock> locks) {
        this.locks = locks;
    }

    @Override
    public void lock() {
        Objects.requireNonNull(locks);
        long waitTime = locks.size() * 1500;//总最大等待时间
        while (true) {
            if (tryLock(waitTime, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
    }

    @Override
    public boolean tryLock() {
        return tryLock(-1, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        if (locks.size() < 3) {
            throw new JedisLockException("More than 3 redis nodes are required");
        }
        long beginTime = System.currentTimeMillis();
        long remainTime = time != -1L ? unit.toMillis(time) : -1L;
        long lockTime = getLockWaitTime(remainTime);
        AtomicInteger acquiredLocks = new AtomicInteger();
        locks.stream().filter(lock -> Objects.nonNull(lock)).forEach(lock -> {
            boolean result;
            try {
                result = time == -1L ? lock.tryLock() : lock.tryLock(lockTime, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                result = false;
            }
            if (result) {
                acquiredLocks.incrementAndGet();
            }
        });
        if (acquiredLocks.get() >= (locks.size() - failedLocksLimit())) {
            long endTime = System.currentTimeMillis() - beginTime;
            if (remainTime != -1L) {
                if ((remainTime - endTime) <= 0L) {
                    unlockInner(locks);
                    return false;
                }
            }
            return true;
        } else {
            unlockInner(locks);
            return false;
        }
    }

    /**
     * 即使某些redis节点根本就没有加锁成功,
     * 但为了防止某些节点获取到锁但是客户端没有得到响应而导致接下来的一段时间不能被重新获取锁
     *
     * @param locks
     */
    protected abstract void unlockInner(List<JedisLock> locks);

    /**
     * 计算每个redis节点的拿锁时间
     *
     * @param remainTime
     * @return
     */
    protected abstract long getLockWaitTime(long remainTime);

    /**
     * 计算允许失败的次数
     *
     * @return
     */
    protected abstract int failedLocksLimit();

    @Override
    public void unlock() {
        locks.forEach(lock -> {
            try {
                lock.unlock();
            } catch (JedisLockException e) {
                //...
            }
        });
    }

    @Override
    public void forceUnlock() {
        locks.forEach(lock -> {
            try {
                lock.forceUnlock();
            } catch (JedisLockException e) {
                //...
            }
        });
    }
}

