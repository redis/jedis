/*
 * Copyright (c) 2010 Nokia Siemens Networks
 * 
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package redis.clients.jedis.pool;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Deals with the Connection pool creation and setting all the configurable pool
 * parameters
 * 
 * @author Ramu Malur S R
 */
abstract class JedisConnectionPoolSupport {

	protected GenericObjectPool connectionPool;

	JedisConnectionPoolSupport(
		String host,
		int port,
		int timeout,
		String password,
		JedisConnectionPoolConfig poolConfig) {

		connectionPool = new GenericObjectPool(new JedisConnectionPoolFactory(
				host, port, timeout, password));
		if (poolConfig != null) {
			connectionPool.setLifo(poolConfig.isLifo());
			connectionPool.setMaxActive(poolConfig.getMaxActive());
			connectionPool.setMaxIdle(poolConfig.getMaxIdle());
			connectionPool.setMaxWait(poolConfig.getMaxWait());
			connectionPool.setMinEvictableIdleTimeMillis(poolConfig
					.getMinEvictableIdleTimeMillis());
			connectionPool.setMinIdle(poolConfig.getMinIdle());
			connectionPool.setNumTestsPerEvictionRun(poolConfig
					.getNumTestsPerEvictionRun());
			connectionPool.setSoftMinEvictableIdleTimeMillis(poolConfig
					.getSoftMinEvictableIdleTimeMillis());
			connectionPool.setTestOnBorrow(poolConfig.isTestOnBorrow());
			connectionPool.setTestOnReturn(poolConfig.isTestOnReturn());
			connectionPool.setTestWhileIdle(poolConfig.isTestWhileIdle());
			connectionPool.setTimeBetweenEvictionRunsMillis(poolConfig
					.getTimeBetweenEvictionRunsMillis());
			byte exhaustAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
			switch (poolConfig.getExhaustAction()) {
				case BLOCK:
					exhaustAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
					break;
				case FAIL:
					exhaustAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
					break;
				case GROW:
					exhaustAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
					break;
				default:
					exhaustAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
					break;
			}
			connectionPool.setWhenExhaustedAction(exhaustAction);
		}
	}
}