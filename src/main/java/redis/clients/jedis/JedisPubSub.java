package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Keyword.MESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PMESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.PUNSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.SUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.UNSUBSCRIBE;

import java.util.Arrays;
import java.util.List;

public abstract class JedisPubSub {
    private int subscribedChannels = 0;
    private Client client;

    public int getSubscribedChannels() {
	return subscribedChannels;
    }

    public boolean isSubscribed() {
	return subscribedChannels > 0;
    }

    public abstract void onMessage(String channel, String message);

    public abstract void onPMessage(String pattern, String channel,
	    String message);

    public abstract void onPSubscribe(String pattern, int subscribedChannels);

    public abstract void onPUnsubscribe(String pattern, int subscribedChannels);

    public abstract void onSubscribe(String channel, int subscribedChannels);

    public abstract void onUnsubscribe(String channel, int subscribedChannels);

    public void proceed(Client client, String... channels) {
	this.client = client;
	client.subscribe(channels);
	process(client);
    }

    public void proceedWithPatterns(Client client, String pattern1,
	    String... patternN) {
	this.client = client;
	// TODO pubsub
	// client.psubscribe(patterns);
	process(client);
    }

    private void process(Client client) {
	do {
	    List<? extends Object> reply = client.getObjectMultiBulkReply();
	    final Object firstObj = reply.get(0);
	    if (!(firstObj instanceof byte[])) {
		throw new JedisException("Unknown message type: " + firstObj);
	    }
	    final byte[] resp = (byte[]) firstObj;
	    if (Arrays.equals(SUBSCRIBE.raw, resp)) {
		subscribedChannels = ((Long) reply.get(2)).intValue();
		final byte[] bchannel = (byte[]) reply.get(1);
		final String strchannel = (bchannel == null) ? null
			: SafeEncoder.encode(bchannel);
		onSubscribe(strchannel, subscribedChannels);
	    } else if (Arrays.equals(UNSUBSCRIBE.raw, resp)) {
		subscribedChannels = ((Long) reply.get(2)).intValue();
		final byte[] bchannel = (byte[]) reply.get(1);
		final String strchannel = (bchannel == null) ? null
			: SafeEncoder.encode(bchannel);
		onUnsubscribe(strchannel, subscribedChannels);
	    } else if (Arrays.equals(MESSAGE.raw, resp)) {
		final byte[] bchannel = (byte[]) reply.get(1);
		final byte[] bmesg = (byte[]) reply.get(2);
		final String strchannel = (bchannel == null) ? null
			: SafeEncoder.encode(bchannel);
		final String strmesg = (bmesg == null) ? null : SafeEncoder
			.encode(bmesg);
		onMessage(strchannel, strmesg);
	    } else if (Arrays.equals(PMESSAGE.raw, resp)) {
		final byte[] bpattern = (byte[]) reply.get(1);
		final byte[] bchannel = (byte[]) reply.get(2);
		final byte[] bmesg = (byte[]) reply.get(3);
		final String strpattern = (bpattern == null) ? null
			: SafeEncoder.encode(bpattern);
		final String strchannel = (bchannel == null) ? null
			: SafeEncoder.encode(bchannel);
		final String strmesg = (bmesg == null) ? null : SafeEncoder
			.encode(bmesg);
		onPMessage(strpattern, strchannel, strmesg);
	    } else if (Arrays.equals(PSUBSCRIBE.raw, resp)) {
		subscribedChannels = ((Long) reply.get(2)).intValue();
		final byte[] bpattern = (byte[]) reply.get(1);
		final String strpattern = (bpattern == null) ? null
			: SafeEncoder.encode(bpattern);
		onPSubscribe(strpattern, subscribedChannels);
	    } else if (Arrays.equals(PUNSUBSCRIBE.raw, resp)) {
		subscribedChannels = ((Long) reply.get(2)).intValue();
		final byte[] bpattern = (byte[]) reply.get(1);
		final String strpattern = (bpattern == null) ? null
			: SafeEncoder.encode(bpattern);
		onPUnsubscribe(strpattern, subscribedChannels);
	    } else {
		throw new JedisException("Unknown message type: " + firstObj);
	    }
	} while (isSubscribed());
    }

    public void psubscribe(String... patterns) {
	client.psubscribe(patterns);
    }

    public void punsubscribe() {
	client.punsubscribe();
    }

    public void punsubscribe(String... patterns) {
	client.punsubscribe(patterns);
    }

    public void subscribe(String... channels) {
	client.subscribe(channels);
    }

    public void unsubscribe() {
	client.unsubscribe();
    }

    public void unsubscribe(String... channels) {
	client.unsubscribe(channels);
    }
}