package redis.clients.jedis;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public class JedisPool {
	private final GenericObjectPool internalPool;

	public JedisPool(final GenericObjectPool.Config poolConfig, final String host) {
		this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null);
	}

	public JedisPool(final GenericObjectPool.Config poolConfig,final String host, final int port) {
		this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null);
	}

	public JedisPool(final GenericObjectPool.Config poolConfig,final String host, final int port, final int timeout) {
		this(poolConfig, host, port, timeout, null);
	}

	public JedisPool(final GenericObjectPool.Config poolConfig,final String host, final int port, final int timeout, final String password) {
		final String lhost;
		final int lport;
		final int ltimeout;
		final String lpassword;
		lhost = host;
		lport = port;
		ltimeout = (timeout > 0) ? timeout : Protocol.DEFAULT_TIMEOUT;
		lpassword = password;
		
		final JedisFactory factory = new JedisFactory(lhost, lport, ltimeout, lpassword);
		this.internalPool = new GenericObjectPool(factory, poolConfig);
	}

	public JedisPool(final GenericObjectPool.Config poolConfig, final JedisShardInfo shardInfo) {
		this(poolConfig, shardInfo.getHost(), shardInfo.getPort(), shardInfo.getTimeout(), shardInfo.getPassword());
	}
	
	public Jedis getResource() throws Exception {
		return (Jedis) internalPool.borrowObject();
	}
	
	public void returnResource(final Jedis jedis) throws Exception {
		internalPool.returnObject(jedis);
	}

	/**
	 * PoolableObjectFactory custom impl.
	 */
	private static class JedisFactory extends BasePoolableObjectFactory {
		private final String host;
		private final int port;
		private final int timeout;
		private final String password;

		public JedisFactory(final String host, final int port, final int timeout, final String password) {
			super();
			this.host = host;
			this.port = port;
			this.timeout = (timeout > 0) ? timeout : -1;
			this.password = password;
		}

		@Override
		public Object makeObject() throws Exception {
			final Jedis jedis;
			if (timeout > 0) {
				jedis = new Jedis(this.host, this.port, this.timeout);
			} else {
				jedis = new Jedis(this.host, this.port);
			}

			jedis.connect();
			if (null != this.password) {
				jedis.auth(this.password);
			}
			return jedis;
		}

		@Override
		public void destroyObject(final Object obj) throws Exception {
			if(obj instanceof Jedis) {
				final Jedis jedis = (Jedis) obj;
				if (jedis.isConnected()) {
					try {
						jedis.quit();
						jedis.disconnect();
					} catch (Exception e) {
	
					}
				}
			}
		}

		@Override
		public boolean validateObject(final Object obj) {
			if(obj instanceof Jedis) {
				final Jedis jedis = (Jedis) obj;
				try {
					return jedis.isConnected() && jedis.ping().equals("PONG");
				} catch (final Exception e) {
					return false;
				}
			} else {
				return false;
			}
		}

		
	}
}
