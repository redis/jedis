package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.util.JedisClusterCRC16;

public abstract class JedisClusterCommand<T> {

    private boolean asking = false;

    private JedisClusterConnectionHandler connectionHandler;
    private int commandTimeout;
    private int redirections;

    // private boolean asking = false;

    public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler,
	    int timeout, int maxRedirections) {
	this.connectionHandler = connectionHandler;
	this.commandTimeout = timeout;
	this.redirections = maxRedirections;
    }

    public abstract T execute();

    public T run(String key) {
	try {

	    if (key == null) {
		throw new JedisClusterException(
			"No way to dispatch this command to Redis Cluster.");
	    } else if (redirections == 0) {
		throw new JedisClusterMaxRedirectionsException(
			"Too many Cluster redirections?");
	    }
	    connectionHandler.getConnectionFromSlot(JedisClusterCRC16
		    .getSlot(key));
	    if (asking) {
		// TODO: Pipeline asking with the original command to make it
		// faster....
		connectionHandler.getConnection().asking();
	    }
	    return execute();
	} catch (JedisRedirectionException jre) {
	    return handleRedirection(jre, key);
	}
    }

    private T handleRedirection(JedisRedirectionException jre, String key) {
	if (jre instanceof JedisAskDataException) {
	    asking = true;
	}
	redirections--;
	this.connectionHandler.assignSlotToNode(jre.getSlot(),
		jre.getTargetNode());
	return run(key);
    }
}