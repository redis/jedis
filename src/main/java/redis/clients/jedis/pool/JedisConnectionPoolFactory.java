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

import org.apache.commons.pool.PoolableObjectFactory;

import redis.clients.jedis.Jedis;

/**
 * Implements Life cycle methods of {@link PoolableObjectFactory}
 * 
 * @author Ramu Malur S R
 */
class JedisConnectionPoolFactory
		implements PoolableObjectFactory {

	private String host;

	private int port;

	private int timeout;

	private String password;

	JedisConnectionPoolFactory(
		String host,
		int port,
		int timeout,
		String password) {

		this.host = host;
		this.port = port;
		this.timeout = timeout;
		this.password = password;
	}

	/**
	 * @see PoolableObjectFactory#activateObject(Object)
	 */
	@Override
	public void activateObject(
		Object client)
		throws Exception {

		// Don't do anything
	}

	/**
	 * @see PoolableObjectFactory#destroyObject(Object)
	 */
	@Override
	public void destroyObject(
		Object client)
		throws Exception {

		Jedis jedisClient = (Jedis) client;
		if (jedisClient != null && jedisClient.isConnected()) {
			jedisClient.quit();
			jedisClient.disconnect();
		}
	}

	/**
	 * @see PoolableObjectFactory#makeObject()
	 */
	@Override
	public Object makeObject()
		throws Exception {

		Jedis jedis = new Jedis(this.host, this.port, this.timeout);
		boolean done = false;
		while (!done) {
			try {
				jedis.connect();
				if (password != null) {
					jedis.auth(password);
				}
				done = true;
			} catch (Exception ex) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
				}
			}
		}
		return jedis;
	}

	/**
	 * @see PoolableObjectFactory#passivateObject(Object)
	 */
	@Override
	public void passivateObject(
		Object client)
		throws Exception {

		// Don't do anything
	}

	/**
	 * @see PoolableObjectFactory#validateObject(Object)
	 */
	@Override
	public boolean validateObject(
		Object client) {

		boolean valid = false;
		try {
			Jedis jedisClient = (Jedis) client;
			if (client != null) {
				valid = jedisClient.isConnected()
						&& jedisClient.ping().equals("PONG");
			}
		} catch (Exception ex) {
			valid = false;
		}
		return valid;
	}
}