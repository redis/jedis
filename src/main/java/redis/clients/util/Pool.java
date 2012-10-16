package redis.clients.util;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public abstract class Pool<T> {
    private GenericObjectPool internalPool;

    public Pool(final GenericObjectPool.Config poolConfig,
            PoolableObjectFactory factory) {
        init(poolConfig, factory);
    }

    public Pool() {
        internalPool = null;
    }

    protected void init(final GenericObjectPool.Config poolConfig,
            PoolableObjectFactory factory) {
        internalPool = new GenericObjectPool(factory, poolConfig);
    }

    @SuppressWarnings("unchecked")
    public T getResource() {
        try {
            return (T) internalPool.borrowObject();
        } catch (Exception e) {
            throw new JedisConnectionException(
                    "Could not get a resource from the pool", e);
        }
    }

    public void returnResource(final T resource) {
        try {
            internalPool.returnObject(resource);
        } catch (Exception e) {
            throw new JedisException(
                    "Could not return the resource to the pool", e);
        }
    }

    public void returnBrokenResource(final T resource) {
        try {
            internalPool.invalidateObject(resource);
        } catch (Exception e) {
            throw new JedisException(
                    "Could not return the resource to the pool", e);
        }
    }

    public void destroy() {
        try {
            internalPool.close();
        } catch (Exception e) {
            throw new JedisException("Could not destroy the pool", e);
        }
    }

    public void clear() {
        try {
            internalPool.clear();
        } catch (Exception e) {
            throw new JedisException("Could not clear the pool", e);
        }
    }

    public int getNumActive() {
        return internalPool.getNumActive();
    }
    public int getNumIdle() {
        return internalPool.getNumIdle();
    }
    public boolean getLifo() {
        return internalPool.getLifo();
    }
    public int getMaxActive() {
        return internalPool.getMaxActive();
    }
    public int getMaxIdle() {
        return internalPool.getMaxIdle();
    }
    public long getMaxWait() {
        return internalPool.getMaxWait();
    }
    public long getMinEvictableIdleTimeMillis() {
        return internalPool.getMinEvictableIdleTimeMillis();
    }
    public int getMinIdle() {
        return internalPool.getMinIdle();
    }
    public int getNumTestsPerEvictionRun() {
        return internalPool.getNumTestsPerEvictionRun();
    }
    public long getSoftMinEvictableIdleTimeMillis() {
        return internalPool.getSoftMinEvictableIdleTimeMillis();
    }
    public boolean getTestOnBorrow() {
        return internalPool.getTestOnBorrow();
    }
    public boolean getTestOnReturn() {
        return internalPool.getTestOnReturn();
    }
    public boolean getTestWhileIdle() {
        return internalPool.getTestWhileIdle();
    }
    public long getTimeBetweenEvictionRunsMillis() {
        return internalPool.getTimeBetweenEvictionRunsMillis();
    }
    public byte getWhenExhaustedActionByte() {
        return internalPool.getWhenExhaustedAction();
    }
    public void setMaxActive(int maxActive) {
        internalPool.setMaxActive(maxActive);
    }
    public void setMaxIdle(int maxIdle) {
        internalPool.setMaxIdle(maxIdle);
    }
    public void setMaxWait(long maxWait) {
        internalPool.setMaxWait(maxWait);
    }
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        internalPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }
    public void setMinIdle(int minIdle) {
        internalPool.setMinIdle(minIdle);
    }
    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        internalPool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }
    public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        internalPool.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
    }
    public void setTestOnBorrow(boolean testOnBorrow) {
        internalPool.setTestOnBorrow(testOnBorrow);
    }
    public void setTestOnReturn(boolean testOnReturn) {
        internalPool.setTestOnReturn(testOnReturn);
    }
    public void setTestWhileIdle(boolean testWhileIdle) {
        internalPool.setTestWhileIdle(testWhileIdle);
    }
    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        internalPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }
    public void setWhenExhaustedActionByte(byte whenExhaustedAction) {
        internalPool.setWhenExhaustedAction(whenExhaustedAction);
    }
}
