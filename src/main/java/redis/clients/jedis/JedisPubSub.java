package redis.clients.jedis;

import java.util.Arrays;
import java.util.List;

import static redis.clients.jedis.Protocol.Keyword.*;

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

    public void unsubscribe() {
        client.unsubscribe();
    }

    public void unsubscribe(String... channels) {
        client.unsubscribe(channels);
    }

    public void subscribe(String... channels) {
        client.subscribe(channels);
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
            final Object firstObj = reply.get(0);
            if (!(firstObj instanceof byte[])) {
            	throw
            		new JedisException("Unknown message type: "+ firstObj);
            }
            final byte[] resp = (byte[]) firstObj;
            if(Arrays.equals(SUBSCRIBE.raw, resp)) {
                subscribedChannels = ((Integer) reply.get(2)).intValue();
                final byte[] bchannel = (byte[]) reply.get(1);
                final String strchannel =
                	(bchannel == null) ?
                			null :
                			new String(bchannel, Protocol.UTF8);
               	onSubscribe(strchannel, subscribedChannels);
            } else if (Arrays.equals(UNSUBSCRIBE.raw, resp)) {
                subscribedChannels = ((Integer) reply.get(2)).intValue();
                final byte[] bchannel = (byte[]) reply.get(1);
                final String strchannel =
                	(bchannel == null) ?
                		null : 
                		new String(bchannel, Protocol.UTF8);
               	onUnsubscribe(strchannel, subscribedChannels);
            } else if (Arrays.equals(MESSAGE.raw, resp)) {
                final byte[] bchannel = (byte[]) reply.get(1);
                final byte[] bmesg = (byte[]) reply.get(2);
                final String strchannel =
                	(bchannel == null) ?
                		null :
                		new String(bchannel, Protocol.UTF8);
                final String strmesg =
                	(bmesg == null) ?
                		null :
                		new String(bmesg, Protocol.UTF8);
                onMessage(strchannel, strmesg);
            } else if (Arrays.equals(PMESSAGE.raw, resp)) {
            	final byte[] bpattern = (byte[]) reply.get(1);
                final byte[] bchannel = (byte[]) reply.get(2);
                final byte[] bmesg = (byte[]) reply.get(3);
                final String strpattern =
                	(bpattern == null) ?
                		null :
                		new String(bpattern, Protocol.UTF8);
                final String strchannel =
                	(bchannel == null) ?
                		null :
                		new String(bchannel, Protocol.UTF8);
                final String strmesg =
                	(bmesg == null) ?
                		null :
                		new String(bmesg, Protocol.UTF8);
                onPMessage(
                		strpattern,
                		strchannel,
                		strmesg);
            } else if (Arrays.equals(PSUBSCRIBE.raw, resp)) {
                subscribedChannels = ((Integer) reply.get(2)).intValue();
            	final byte[] bpattern = (byte[]) reply.get(1);
                final String strpattern =
                	(bpattern == null) ?
                		null :
                		new String(bpattern, Protocol.UTF8);
                onPSubscribe(strpattern, subscribedChannels);
            } else if (Arrays.equals(PUNSUBSCRIBE.raw, resp)) {
                subscribedChannels = ((Integer) reply.get(2)).intValue();
            	final byte[] bpattern = (byte[]) reply.get(1);
                final String strpattern =
                	(bpattern == null) ?
                		null :
                		new String(bpattern, Protocol.UTF8);
                onPUnsubscribe(strpattern, subscribedChannels);
            } else {
                throw new JedisException("Unknown message type: "+ firstObj);
            }
        } while (isSubscribed());
    }

    public int getSubscribedChannels() {
        return subscribedChannels;
    }
}