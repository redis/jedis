package redis.clients.jedis.util.lock;

import redis.clients.jedis.exceptions.JedisLockException;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

public class JedisReentrantLock implements JedisLock {
    private String name;
    private LockCommand client;
    private boolean isListener;
    /**
     * watchdog,update lock ttl
     */
    private Future<?> future;
    private String lockScript, unLockScript, forceUnLockScript, updateTTLScript;
    private ThreadLocal<String> threadLocal = new ThreadLocal<>();
    private Set<Thread> subscribers = Collections.synchronizedSet(new HashSet<Thread>());
    private ScheduledExecutorService watchGroup = Executors.newScheduledThreadPool(10);

    protected JedisReentrantLock(String name, LockCommand client) {
        this.name = name;
        this.client = client;
    }


    @Override
    public void lock() {
        lock(acquireVisitorId());
    }

    private void lock(String visitorId) {
        Long ttl = acquireLock(visitorId);
        if (ttl == -1) {
            watchDog(visitorId); //add watchdog
            return;
        }
        subscribe();
        try {
            while (true) {
                ttl = acquireLock(visitorId);
                if (ttl == -1) {
                    watchDog(visitorId);
                    break;
                }
                if (ttl >= 0) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(ttl));
                }
            }
        } finally {
            unsubscribe();
        }
    }

    @Override
    public boolean tryLock() {
        return tryLock(acquireVisitorId());
    }

    private boolean tryLock(String visitorId) {
        boolean result = false;
        if (acquireLock(visitorId) == -1) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        return tryLock(acquireVisitorId(), time, unit);
    }

    private boolean tryLock(String visitorId, long time, TimeUnit unit) {
        Objects.requireNonNull(unit);
        if (time < 0) {
            throw new RuntimeException("Parameter time must be >= 0");
        }
        Long ttl = acquireLock(visitorId);
        if (ttl == -1) {
            watchDog(visitorId);
            return true;
        }
        subscribe();
        try {
            LockSupport.parkNanos(unit.toNanos(time));
            ttl = acquireLock(visitorId);
            if (ttl == -1) {
                watchDog(visitorId);
                return true;
            }
        } finally {
            unsubscribe();
        }
        return false;
    }


    @Override
    public void unlock() {
        unlock(acquireVisitorId());
    }

    private void unlock(String visitorId) {
        synchronized (this) {
            if (null == unLockScript) {
                unLockScript = client.scriptLoad(Constants.ACQUIRE_UNLOCK_SCRIPT);
            }
        }
        Long result = null;
        try {
            result = (Long) client.evalsha(unLockScript, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL), visitorId);
        } catch (JedisNoScriptException e) {
            result = (Long) client.eval(Constants.ACQUIRE_UNLOCK_SCRIPT, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL), visitorId);
        } finally {
            if (null == result) {
                threadLocal.remove();//reset visitorId
                if (null != future) {
                    future.cancel(true);//watchdog exit
                }
                return;
            }
            if (result == 1) {
                if (null != future) {
                    future.cancel(true);
                }
            } else if (result == 0) {
                throw new JedisLockException(String.format("attempt to unlock lock, not locked by " +
                        "current thread by visitor id: %s", acquireVisitorId()));
            }
        }
    }

    @Override
    public void forceUnlock() {
        synchronized (this) {
            if (null == forceUnLockScript) {
                forceUnLockScript = client.scriptLoad(Constants.ACQUIRE_FORCE_UNLOCK_SCRIPT);
            }
        }
        Long result = null;
        try {
            result = (Long) client.evalsha(forceUnLockScript, 1, name);
        } catch (JedisNoScriptException e) {
            result = (Long) client.eval(Constants.ACQUIRE_FORCE_UNLOCK_SCRIPT, 1, name);
        } catch (ClassCastException e) {
            //...
        } finally {
            if (result == 1) {
                if (null != future) {
                    future.cancel(true);
                }
            }
        }
    }

    private void watchDog(final String visitorId) {
        if (null != future) {
            future.cancel(true);
        }
        future = watchGroup.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (null == updateTTLScript) {
                    updateTTLScript = client.scriptLoad(Constants.UPDATE_LOCK_TTL_SCRIPT);
                }
                try {
                    client.evalsha(updateTTLScript, 1, name, visitorId,
                            String.valueOf(Constants.DEFAULT_KEY_TTL));
                } catch (JedisNoScriptException e) {
                    client.eval(Constants.UPDATE_LOCK_TTL_SCRIPT, 1, name, visitorId,
                            String.valueOf(Constants.DEFAULT_KEY_TTL));
                }
            }
        }, Constants.DEFAULT_UPDATE_TIME, Constants.DEFAULT_UPDATE_TIME, TimeUnit.SECONDS);
    }

    private void subscribe() {
        Thread thread = Thread.currentThread();
        synchronized (subscribers) {
            if (!isListener) {
                isListener = true;
                new Thread() {
                    @Override
                    public void run() {
                        client.subscribe(future, new SubscribeListener(subscribers), name);
                    }
                }.start();
            }
            subscribers.add(thread);
        }
    }

    private void unsubscribe() {
        Thread thread = Thread.currentThread();
        synchronized (subscribers) {
            if (subscribers.contains(thread)) {
                subscribers.remove(thread);
            }
        }
    }

    private String acquireVisitorId() {
        String visitorId = threadLocal.get();
        if (null == visitorId) {
            visitorId = String.format("%s:%s", UUID.randomUUID().toString(),
                    Thread.currentThread().getId());//visitorId = uuid+threadId
            threadLocal.set(visitorId);
        }
        return visitorId;
    }

    private Long acquireLock(String visitorId) {
        synchronized (this) {
            if (null == lockScript) {
                lockScript = client.scriptLoad(Constants.ACQUIRE_LOCK_SCRIPT);
            }
        }
        try {
            return (Long) client.evalsha(lockScript, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL));
        } catch (ClassCastException e) {
            return 100L;
        } catch (JedisNoScriptException e) {
            return (Long) client.eval(Constants.ACQUIRE_LOCK_SCRIPT, 1, name, visitorId,
                    String.valueOf(Constants.DEFAULT_KEY_TTL));
        }
    }
}
