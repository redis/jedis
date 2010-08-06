package redis.clients.jedis;

public abstract class JedisPipeline {
    protected Client client;

    public void setClient(Client client) {
	this.client = client;
    }

    public abstract void execute();
}
