package redis.clients.jedis.exceptions;

import redis.clients.jedis.HostAndPort;

public class JedisAskDataException extends JedisDataException {
    private static final long serialVersionUID = 3878126572474819403L;
    
    private HostAndPort targetNode;
    private int slot;

    public JedisAskDataException(Throwable cause) {
        super(cause);
    }

    public JedisAskDataException(String message, Throwable cause) {
        super(message, cause);
    }

	public JedisAskDataException(String message, HostAndPort targetHost, int slot) {
		super(message);
		this.targetNode = targetHost; 
		this.slot = slot;
	}

	public HostAndPort getTargetNode() {
		return targetNode;
	}

	public int getSlot() {
		return slot;
	}
}
