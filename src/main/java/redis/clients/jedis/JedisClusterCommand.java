package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.tests.utils.JedisClusterCRC16;

public abstract class JedisClusterCommand<T> {
	
	private boolean asking = false;
	
	private JedisClusterConnectionHandler connectionHandler;
//	private boolean asking = false;
	
	public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	public abstract T execute();
	
	public T run(String key) {
		try {
			if (key == null) {
				throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
			}
			connectionHandler.getConnectionFromSlot(JedisClusterCRC16.getSlot(key));
			if (asking) {
				//TODO: Pipeline asking with the original command to make it faster....
				connectionHandler.getConnection().asking();
			}
			return execute();
		} catch (JedisMovedDataException jme) {
			this.connectionHandler.assignSlotToNode(jme.getSlot(), jme.getTargetNode());
			return run(key);
		} catch (JedisAskDataException jae) {
			asking = true;
			this.connectionHandler.assignSlotToNode(jae.getSlot(), jae.getTargetNode());
			return run(key);
		}
	}
}
