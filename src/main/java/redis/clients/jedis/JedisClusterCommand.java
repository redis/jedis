package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;

public abstract class JedisClusterCommand<T> {
	
	private JedisClusterConnectionHandler connectionHandler;
	
	public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	public abstract T execute();
	
	public T run() {
		try {
			return execute();
		} catch (JedisMovedDataException jme) {
			//TODO: Do Retry here
		} catch (JedisAskDataException jae) {
			//TODO: Do ASK here
		}
		return null;
	}
}
