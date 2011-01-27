package com.googlecode.jedis.util;

import java.io.IOException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.target.CommonsPoolTargetSource;

import com.googlecode.jedis.Jedis;

/**
 * Target source to use with spring.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public class JedisPoolTargetSource extends CommonsPoolTargetSource {

    Logger log = LoggerFactory.getLogger(JedisPoolTargetSource.class);

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.aop.target.CommonsPoolTargetSource#createObjectPool()
     */
    @Override
    protected ObjectPool createObjectPool() {
	GenericObjectPool gop = new GenericObjectPool(this);
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
    protected void destroyPrototypeInstance(Object target) {
	super.destroyPrototypeInstance(target);

	if (target instanceof Jedis) {
	    Jedis jedis = (Jedis) target;
	    try {
		jedis.disconnect();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
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
    public boolean validateObject(Object obj) {
	Boolean valid = false;

	log.debug("validate object");

	if (obj instanceof Jedis) {
	    log.debug("Validation of a Jedis instance");
	    Jedis jedis = (Jedis) obj;
	    valid = jedis.isConnected();
	}

	return valid;
    }

}
