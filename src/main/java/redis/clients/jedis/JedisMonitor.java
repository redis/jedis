package redis.clients.jedis;

public abstract class JedisMonitor {
    protected Client client;

    public void proceed(Client client) {
	this.client = client;
	this.client.setTimeoutInfinite();
	do {
	    String command = client.getBulkReply();
	    onCommand(command);
	} while (client.isConnected());
    }

    public abstract void onCommand(String command);
}