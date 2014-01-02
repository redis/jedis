package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;

public abstract class JedisClusterCommand<T> {
	
	private JedisClusterConnectionHandler connectionHandler;
	private boolean asking = false;
	
	public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	public abstract T execute();
	
	public T run() {
		try {
			return execute();
		} catch (JedisMovedDataException jme) {
			this.connectionHandler.assignSlotToNode(jme.getSlot(), jme.getTargetNode());
			return execute();
		} catch (JedisAskDataException jae) {
			throw jae;
		}
	}
}
