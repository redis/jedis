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

/**
 * Jedis Connection Pool Configuration. It caters to all Configurations
 * Supported by Apache Commons Pool
 * 
 * @see "http://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.html"
 *      for details of each Configuration and its effect on Pool behavior
 * @author Ramu Malur S R
 */
public class JedisConnectionPoolConfig {

	private JedisConnectionPoolExhaustAction exhaustAction = JedisConnectionPoolExhaustAction.FAIL;

	private boolean lifo = false;

	private int maxActive = 8;

	private int maxIdle = 8;

	private long maxWait = 5 * 60 * 1000l;

	private long minEvictableIdleTimeMillis = 30 * 60 * 1000l;

	private int minIdle = 0;

	private int numTestsPerEvictionRun = 3;

	private long softMinEvictableIdleTimeMillis = -1;

	private boolean testOnBorrow = true;

	private boolean testOnReturn = false;

	private boolean testWhileIdle = false;

	private long timeBetweenEvictionRunsMillis = -1;

	/**
	 * @return Returns {@link JedisConnectionPoolExhaustAction} action to take
	 *         when the pool is exhausted
	 */
	public JedisConnectionPoolExhaustAction getExhaustAction() {

		return exhaustAction;
	}

	/**
	 * Maximum number of connections that can be allocated by the pool (checked
	 * out to clients, or idle awaiting checkout) at a given time. When
	 * non-positive, there is no limit to the number of connections that can be
	 * managed by the pool at one time. When maxActive is reached, the pool is
	 * said to be exhausted. The default setting for this parameter is 8
	 * 
	 * @return the number of maximum active connections
	 */
	public int getMaxActive() {

		return maxActive;
	}

	/**
	 * Maximum number of connections that can sit idle in the pool at any time.
	 * When negative, there is no limit to the number of connections that may be
	 * idle at one time. The default setting for this parameter is 8
	 * 
	 * @return the number of maximum idle connections
	 */
	public int getMaxIdle() {

		return maxIdle;
	}

	/**
	 * Returns the maximum amount of time (in milliseconds) the
	 * {@link JedisConnectionPool#borrow()} method should block before throwing
	 * an exception when the pool is exhausted and the "exhaustAction" is
	 * {@link JedisConnectionPoolExhaustAction#BLOCK}
	 * 
	 * @return the maximum wait time for a connection
	 */
	public long getMaxWait() {

		return maxWait;
	}

	/**
	 * Returns the minimum amount of time that a connection may sit idle in the
	 * pool before it is eligible for eviction due to idle time. When
	 * non-positive, no connection will be dropped from the pool due to idle
	 * time alone. This setting has no effect unless
	 * timeBetweenEvictionRunsMillis > 0. The default setting for this parameter
	 * is 30 minutes.
	 * 
	 * @return the minEvictableIdleTimeMillis configuration
	 */
	public long getMinEvictableIdleTimeMillis() {

		return minEvictableIdleTimeMillis;
	}

	/**
	 * Returns the minimum number of connections allowed in the pool before the
	 * evictor thread (if active) spawns new connections.
	 * 
	 * @return the minIdle configuration
	 */
	public int getMinIdle() {

		return minIdle;
	}

	/**
	 * Returns the max number of connections to examine during each run of the
	 * idle connection evictor thread (if any).
	 * 
	 * @return the numTestsPerEvictionRun configuration
	 */
	public int getNumTestsPerEvictionRun() {

		return numTestsPerEvictionRun;
	}

	/**
	 * Returns the minimum amount of time an connection may sit idle in the pool
	 * before it is eligible for eviction by the idle connection evictor (if
	 * any), with the extra condition that at least "minIdle" amount of
	 * connections remain in the pool.
	 * 
	 * @return the softMinEvictableIdleTimeMillis configuration
	 */
	public long getSoftMinEvictableIdleTimeMillis() {

		return softMinEvictableIdleTimeMillis;
	}

	/**
	 * Returns the number of milliseconds to sleep between runs of the idle
	 * connection evictor thread.
	 * 
	 * @return the timeBetweenEvictionRunsMillis configuration
	 */
	public long getTimeBetweenEvictionRunsMillis() {

		return timeBetweenEvictionRunsMillis;
	}

	/**
	 * Whether or not the idle object pool acts as a LIFO queue.
	 * 
	 * @return the lifo configuration
	 */
	public boolean isLifo() {

		return lifo;
	}

	/**
	 * Returns true, if the pool has to attempt to validate each connection
	 * before it is returned from the {@link JedisConnectionPool#borrow()}
	 * method. Jedis Connections that fail to validate will be dropped from the
	 * pool, and a different connection will be borrowed. Returns false if
	 * validation is not required. The default setting for this parameter is
	 * false.
	 * 
	 * @return the testOnBorrow configuration
	 */
	public boolean isTestOnBorrow() {

		return testOnBorrow;
	}

	/**
	 * Returns true, if the pool has to attempt to validate each connection
	 * before it is returned to the pool using
	 * {@link JedisConnectionPool#release(redis.clients.jedis.Jedis)}.
	 * Connections that fail to validate will be dropped from the pool. Returns
	 * false if this validation is not required. The default setting for this
	 * parameter is false.
	 * 
	 * @return the testOnReturn configuration
	 */
	public boolean isTestOnReturn() {

		return testOnReturn;
	}

	/**
	 * Returns true which indicates that idle connections should be validated.
	 * Returns false if this validation is not required. This setting has no
	 * effect unless timeBetweenEvictionRunsMillis > 0. The default setting for
	 * this parameter is false.
	 * 
	 * @return the testWhileIdle configuration
	 */
	public boolean isTestWhileIdle() {

		return testWhileIdle;
	}

	/**
	 * {@link JedisConnectionPoolExhaustAction} to set
	 * 
	 * @param exhaustAction
	 */
	public void setExhaustAction(
		JedisConnectionPoolExhaustAction exhaustAction) {

		this.exhaustAction = exhaustAction;
	}

	/**
	 * Sets lifo configuration of pool
	 * 
	 * @param lifo
	 */
	public void setLifo(
		boolean lifo) {

		this.lifo = lifo;
	}

	/**
	 * Sets maxActive configuration of pool
	 * 
	 * @param maxActive
	 */
	public void setMaxActive(
		int maxActive) {

		this.maxActive = maxActive;
	}

	/**
	 * Sets maxIdle configuration of pool
	 * 
	 * @param maxIdle
	 */
	public void setMaxIdle(
		int maxIdle) {

		this.maxIdle = maxIdle;
	}

	/**
	 * Sets maxWait configuration of pool
	 * 
	 * @param maxWait
	 */
	public void setMaxWait(
		long maxWait) {

		this.maxWait = maxWait;
	}

	/**
	 * Sets minEvictableIdleTimeMillis configuration of pool
	 * 
	 * @param minEvictableIdleTimeMillis
	 */
	public void setMinEvictableIdleTimeMillis(
		long minEvictableIdleTimeMillis) {

		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * Sets minIdle configuration of pool
	 * 
	 * @param minIdle
	 */
	public void setMinIdle(
		int minIdle) {

		this.minIdle = minIdle;
	}

	/**
	 * Sets numTestsPerEvictionRun configuration of pool
	 * 
	 * @param numTestsPerEvictionRun
	 */
	public void setNumTestsPerEvictionRun(
		int numTestsPerEvictionRun) {

		this.numTestsPerEvictionRun = numTestsPerEvictionRun;
	}

	/**
	 * Sets softMinEvictableIdleTimeMillis configuration of pool
	 * 
	 * @param softMinEvictableIdleTimeMillis
	 */
	public void setSoftMinEvictableIdleTimeMillis(
		long softMinEvictableIdleTimeMillis) {

		this.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
	}

	/**
	 * Sets testOnBorrow configuration of pool
	 * 
	 * @param testOnBorrow
	 */
	public void setTestOnBorrow(
		boolean testOnBorrow) {

		this.testOnBorrow = testOnBorrow;
	}

	/**
	 * Sets testOnReturn configuration of pool
	 * 
	 * @param testOnReturn
	 */
	public void setTestOnReturn(
		boolean testOnReturn) {

		this.testOnReturn = testOnReturn;
	}

	/**
	 * Sets testWhileIdle configuration of pool
	 * 
	 * @param testWhileIdle
	 */
	public void setTestWhileIdle(
		boolean testWhileIdle) {

		this.testWhileIdle = testWhileIdle;
	}

	/**
	 * Sets timeBetweenEvictionRunsMillis configuration of pool
	 * 
	 * @param timeBetweenEvictionRunsMillis
	 */
	public void setTimeBetweenEvictionRunsMillis(
		long timeBetweenEvictionRunsMillis) {

		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder strBldr = new StringBuilder();

		strBldr.append("Pool Configuration: maxActive=");
		strBldr.append(this.maxActive);
		strBldr.append(", maxIdle=");
		strBldr.append(this.maxIdle);
		strBldr.append(", minIdle=");
		strBldr.append(this.minIdle);
		strBldr.append(", maxWait=");
		strBldr.append(this.maxWait);
		strBldr.append("ms, minEvictableIdleTimeMillis=");
		strBldr.append(this.minEvictableIdleTimeMillis);
		strBldr.append("ms, softMinEvictableIdleTimeMillis=");
		strBldr.append(this.softMinEvictableIdleTimeMillis);
		strBldr.append("ms, numTestsPerEvictionRun=");
		strBldr.append(this.numTestsPerEvictionRun);
		strBldr.append(", timeBetweenEvictionRunsMillis=");
		strBldr.append(this.timeBetweenEvictionRunsMillis);
		strBldr.append("ms, lifo=");
		strBldr.append(this.lifo);
		strBldr.append(", testOnBorrow=");
		strBldr.append(this.testOnBorrow);
		strBldr.append(", testOnReturn=");
		strBldr.append(this.testOnReturn);
		strBldr.append(", testWhileIdle=");
		strBldr.append(this.testWhileIdle);

		return strBldr.toString();
	}
}