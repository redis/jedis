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

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;

/**
 * A pool of {@link Jedis} connections. It supports various configuration
 * options through {@link JedisConnectionPoolConfig}
 * 
 * @author Ramu Malur S R
 */
public class JedisConnectionPool
		extends JedisConnectionPoolSupport {

	private static final int DEFAULT_TIMEOUT = 5000;

	/**
	 * @param host
	 * @param port
	 * @param timeout
	 * @param password
	 * @param poolConfig
	 */
	public JedisConnectionPool(
		String host,
		int port,
		int timeout,
		String password,
		JedisConnectionPoolConfig poolConfig) {

		super(host, port, timeout, password, poolConfig);
	}

	/**
	 * @param host
	 * @param poolConfig
	 */
	public JedisConnectionPool(
		String host,
		JedisConnectionPoolConfig poolConfig) {

		this(host, Protocol.DEFAULT_PORT, DEFAULT_TIMEOUT, null, poolConfig);
	}

	/**
	 * @param host
	 * @param port
	 * @param poolConfig
	 */
	public JedisConnectionPool(
		String host,
		int port,
		JedisConnectionPoolConfig poolConfig) {

		this(host, port, DEFAULT_TIMEOUT, null, poolConfig);
	}

	/**
	 * @param host
	 * @param port
	 * @param timeout
	 * @param poolConfig
	 */
	public JedisConnectionPool(
		String host,
		int port,
		int timeout,
		JedisConnectionPoolConfig poolConfig) {

		this(host, port, timeout, null, poolConfig);
	}

	/**
	 * @param shardInfo
	 * @param poolConfig
	 */
	public JedisConnectionPool(
		JedisShardInfo shardInfo,
		JedisConnectionPoolConfig poolConfig) {

		this(shardInfo.getHost(), shardInfo.getPort(), shardInfo.getTimeout(),
				shardInfo.getPassword(), poolConfig);
	}

	/**
	 * Borrow {@link Jedis} client from Pool
	 * 
	 * @return An instance of {@link Client}
	 * @throws JedisException
	 *             If there are issues in getting the client
	 */
	public Jedis borrow()
		throws JedisException {

		Jedis connection = null;
		try {
			connection = (Jedis) connectionPool.borrowObject();
		} catch (Exception ex) {
			throw new JedisException(
					"Could not borrow Jedis Connection from pool", ex);
		}
		return connection;
	}

	/**
	 * Release the {@link Jedis} client to pool
	 * 
	 * @param client
	 *            Client to be released
	 * @throws JedisException
	 *             If there are issues in releasing client to pool
	 */
	public void release(
		Jedis client)
		throws JedisException {

		try {
			connectionPool.returnObject(client);
		} catch (Exception ex) {
			throw new JedisException(
					"Could not release Jedis Connection to pool", ex);
		}
	}

	/**
	 * Validates the Client
	 * 
	 * @param client
	 * @return true if Client is still valid, false otherwise
	 */
	public boolean isValid(
		Jedis client) {

		boolean valid = false;
		try {
			if (client != null) {
				valid = client.isConnected() && client.ping().equals("PONG");
			}
		} catch (Exception ex) {
			valid = false;
		}
		return valid;
	}

	/**
	 * Destroys the Pool (Releases all idle connections before destroying)
	 * 
	 * @throws JedisException
	 */
	public void destroyPool()
		throws JedisException {

		connectionPool.clear();
		try {
			connectionPool.close();
		} catch (Exception ex) {
			throw new JedisException(ex.getMessage(), ex);
		}
	}
}