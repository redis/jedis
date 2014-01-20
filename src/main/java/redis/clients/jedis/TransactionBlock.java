package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisException;

public abstract class TransactionBlock extends Transaction {
	// For shadowing
	@SuppressWarnings("unused")
	private Client client;	
	
    public TransactionBlock(Client client) {
	super(client);
    }

    public TransactionBlock() {
    }

    public abstract void execute() throws JedisException;

    public void setClient(Client client) {
	    super.setClient(client);
    }
}
