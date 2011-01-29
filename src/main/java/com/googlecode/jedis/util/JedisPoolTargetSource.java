package com.googlecode.jedis.util;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.springframework.aop.target.CommonsPoolTargetSource;

import com.googlecode.jedis.Jedis;

/**
 * Target source to use with spring.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public class JedisPoolTargetSource extends CommonsPoolTargetSource {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.aop.target.CommonsPoolTargetSource#createObjectPool()
     */
    @Override
    protected ObjectPool createObjectPool() {
	final GenericObjectPool gop = new GenericObjectPool(this);
	gop.setMaxActive(getMaxSize());
	gop.setMaxIdle(getMaxIdle());
	gop.setMinIdle(getMinIdle());
	gop.setMaxWait(getMaxWait());
	gop.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
	gop.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
	gop.setWhenExhaustedAction(getWhenExhaustedAction());
	gop.setTestOnBorrow(true);
	return gop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.target.AbstractPrototypeBasedTargetSource#
     * destroyPrototypeInstance(java.lang.Object)
     */
    @Override
    protected void destroyPrototypeInstance(final Object target) {
	super.destroyPrototypeInstance(target);

	if (target instanceof Jedis) {
	    final Jedis jedis = (Jedis) target;
	    jedis.disconnect();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.aop.target.CommonsPoolTargetSource#validateObject
     * (java.lang.Object)
     */
    @Override
    public boolean validateObject(final Object obj) {
	Boolean valid = false;

	if (obj instanceof Jedis) {
	    final Jedis jedis = (Jedis) obj;
	    valid = jedis.isConnected();
	}

	return valid;
    }

}
