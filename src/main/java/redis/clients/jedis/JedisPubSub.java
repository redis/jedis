package redis.clients.jedis;

import java.util.List;

public abstract class JedisPubSub {
    private int subscribedChannels = 0;
    private Client client;

    public abstract void onMessage(String channel, String message);

    public abstract void onPMessage(String pattern, String channel,
	    String message);

    public abstract void onSubscribe(String channel, int subscribedChannels);

    public abstract void onUnsubscribe(String channel, int subscribedChannels);

    public abstract void onPUnsubscribe(String pattern, int subscribedChannels);

    public abstract void onPSubscribe(String pattern, int subscribedChannels);

    protected void unsubscribe() {
	client.unsubscribe();
    }

    protected void unsubscribe(String... channels) {
	client.unsubscribe(channels);
    }

    protected void subscribe(String... channels) {
	client.subscribe(channels);
    }

    public boolean isSubscribed() {
	return subscribedChannels > 0;
    }

    public void proceedWithPatterns(Client client, String... patterns) {
	this.client = client;
	client.psubscribe(patterns);
	process(client);
    }

    public void proceed(Client client, String... channels) {
	this.client = client;
	client.subscribe(channels);
	process(client);
    }

    private void process(Client client) {
	do {
	    List<Object> reply = client.getObjectMultiBulkReply();
	    if (reply.get(0).equals("subscribe")) {
		subscribedChannels = ((Integer) reply.get(2)).intValue();
		onSubscribe((String) reply.get(1), subscribedChannels);
	    } else if (reply.get(0).equals("unsubscribe")) {
		subscribedChannels = ((Integer) reply.get(2)).intValue();
		onUnsubscribe((String) reply.get(1), subscribedChannels);
	    } else if (reply.get(0).equals("message")) {
		onMessage((String) reply.get(1), (String) reply.get(2));
	    } else if (reply.get(0).equals("pmessage")) {
		onPMessage((String) reply.get(1), (String) reply.get(2),
			(String) reply.get(3));
	    } else if (reply.get(0).equals("psubscribe")) {
		subscribedChannels = ((Integer) reply.get(2)).intValue();
		onPSubscribe((String) reply.get(1), subscribedChannels);
	    } else if (reply.get(0).equals("punsubscribe")) {
		subscribedChannels = ((Integer) reply.get(2)).intValue();
		onPUnsubscribe((String) reply.get(1), subscribedChannels);
	    } else {
		throw new JedisException("Unknown message type: "
			+ reply.get(0));
	    }
	} while (isSubscribed());
    }

    protected void punsubscribe() {
	client.punsubscribe();
    }

    protected void punsubscribe(String... patterns) {
	client.punsubscribe(patterns);
    }
}
