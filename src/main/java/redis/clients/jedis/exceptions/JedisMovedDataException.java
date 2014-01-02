package redis.clients.jedis.exceptions;

import redis.clients.jedis.HostAndPort;


public class JedisMovedDataException extends JedisDataException {
    private static final long serialVersionUID = 3878126572474819403L;
    
    private HostAndPort targetNode;
    private int slot;

    public JedisMovedDataException(String message, HostAndPort targetNode, int slot) {
    	super(message);
    	this.targetNode = targetNode;
    	this.slot = slot;
    }

    public JedisMovedDataException(Throwable cause, HostAndPort targetNode, int slot) {
        super(cause);
        this.targetNode = targetNode;
    	this.slot = slot;
    }

    public JedisMovedDataException(String message, Throwable cause, HostAndPort targetNode, int slot) {
        super(message, cause);
        this.targetNode = targetNode;
    	this.slot = slot;
    }

	public HostAndPort getTargetNode() {
		return targetNode;
	}

	public int getSlot() {
		return slot;
	}
}
