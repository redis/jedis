package redis.clients.jedis;

import java.io.IOException;
import java.net.UnknownHostException;

import redis.clients.util.DynamicResourcePool;

/**
 * @author secmask@gmail.com
 *
 */
public class JedisDynamicPool extends DynamicResourcePool<Jedis> {
	private String	host;
	private int		port;
	private int		timeout;

	public JedisDynamicPool(String host) {
		this(host, Protocol.DEFAULT_PORT);
	}

	public JedisDynamicPool(String host, int port) {
		this(host, port, 5000, 30000L, 30000L);
	}

	public JedisDynamicPool(String host, int port, int timeout, long maxResourceIdleTime,
			long cleanupDelay) {
		super(maxResourceIdleTime, cleanupDelay);
		this.host = host;
		this.port = port;
		this.timeout = timeout;

	}

	@Override
	protected Jedis createResource() {
		Jedis jedis = new Jedis(this.host, this.port, this.timeout);
		try {
			jedis.connect();
		} catch (UnknownHostException e) {
			throw new JedisException(e);
		} catch (IOException e) {
			throw new JedisException(e);
		}
		return jedis;
	}

	@Override
	protected void destroyResource(Jedis jedis) {
		try {
			jedis.quit();
			jedis.disconnect();
		} catch (Exception e) {
			// silent ignore any error.
			handleException(e);
		}
	}

	@Override
	protected boolean isResourceValid(Jedis jedis) {
		try {
			String val = jedis.ping();
			return val.equals("PONG");
		} catch (Exception e) {
			handleException(e);
		}
		return false;
	}
}
