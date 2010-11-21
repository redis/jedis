package redis.clients.util;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public abstract class Pool<T> {
    private final GenericObjectPool internalPool;

    public Pool(final GenericObjectPool.Config poolConfig,
            PoolableObjectFactory factory) {
        this.internalPool = new GenericObjectPool(factory, poolConfig);
    }

    @SuppressWarnings("unchecked")
    public T getResource() throws Exception {
        return (T) internalPool.borrowObject();
    }

    public void returnResource(final T resource) throws Exception {
        internalPool.returnObject(resource);
    }

    public void returnBrokenResource(final T resource) throws Exception {
        internalPool.invalidateObject(resource);
    }

    public void destroy() throws Exception {
        internalPool.close();
    }
}