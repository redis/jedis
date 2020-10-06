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

/**
 * @author gao_xianglong@sina.com
 * @version 0.1-SNAPSHOT
 * @date created in 2020/10/6 3:38 下午
 */
public class JedisRedLock extends JedisMultiLock{
    public JedisRedLock(List<JedisLock> locks) {
        super(locks);
    }

    @Override
    protected void unlockInner(List<JedisLock> locks) {
        Objects.requireNonNull(locks);
        locks.forEach(lock -> {
            try {
                lock.unlock();
            } catch (JedisLockException e) {
                //...
            }
        });
    }

    @Override
    protected long getLockWaitTime(long remainTime) {
        return Math.max(remainTime / locks.size(), 1);
    }

    @Override
    protected int failedLocksLimit() {
        return locks.size() - ((locks.size() >> 1) + 1);
    }
}
