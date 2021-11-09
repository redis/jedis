package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Keyword.MESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PMESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.PUNSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.SUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.UNSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.PONG;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public abstract class JedisPubSub {

    private static final String JEDIS_SUBSCRIPTION_MESSAGE = "JedisPubSub is not subscribed to a Jedis instance.";
    private int subscribedChannels = 0;
    private volatile Connection connection;


    public void onMessage(String channel, String message) {
    }

    public void onPMessage(String pattern, String channel, String message) {
    }

    public void onSubscribe(String channel, int subscribedChannels) {
    }

    public void onUnsubscribe(String channel, int subscribedChannels) {
    }

    public void onPUnsubscribe(String pattern, int subscribedChannels) {
    }

    public void onPSubscribe(String pattern, int subscribedChannels) {
    }

    public void onPong(String pattern) {

    }

    public void unsubscribe() {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.UNSUBSCRIBE);
        connection.flush();
    }

    public void unsubscribe(String... channels) {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.UNSUBSCRIBE, channels);
        connection.flush();
    }

    public void subscribe(String... channels) {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.SUBSCRIBE, channels);
        connection.flush();
    }

    public void psubscribe(String... patterns) {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.PSUBSCRIBE, patterns);
        connection.flush();
    }

    public void punsubscribe() {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.PUNSUBSCRIBE);
        connection.flush();
    }

    public void punsubscribe(String... patterns) {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.PUNSUBSCRIBE, patterns);
        connection.flush();
    }

    public void ping() {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.ping();
        connection.flush();
    }

    public void ping(String argument) {
        if (connection == null) {
            throw new JedisConnectionException(JEDIS_SUBSCRIPTION_MESSAGE);
        }
        connection.sendCommand(Protocol.Command.PING, argument);
        connection.flush();
    }

    public boolean isSubscribed() {
        return subscribedChannels > 0;
    }

    public void proceedWithPatterns(Connection connection, String... patterns) {
        this.connection = connection;
        connection.sendCommand(Protocol.Command.PSUBSCRIBE, patterns);
        connection.flush();
        process(connection);
    }

    public void proceed(Connection connection, String... channels) {
        this.connection = connection;
        connection.sendCommand(Protocol.Command.SUBSCRIBE, channels);
        connection.flush();
        process(connection);
    }

    private void process(Connection connection) {

        do {
            List<Object> reply = connection.getUnflushedObjectMultiBulkReply();
            final Object firstObj = reply.get(0);
            if (!(firstObj instanceof byte[])) {
                throw new JedisException("Unknown message type: " + firstObj);
            }
            final byte[] resp = (byte[]) firstObj;
            if (Arrays.equals(SUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bchannel = (byte[]) reply.get(1);
                final String strchannel = (bchannel == null) ? null : SafeEncoder.encode(bchannel);
                onSubscribe(strchannel, subscribedChannels);
            } else if (Arrays.equals(UNSUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bchannel = (byte[]) reply.get(1);
                final String strchannel = (bchannel == null) ? null : SafeEncoder.encode(bchannel);
                onUnsubscribe(strchannel, subscribedChannels);
            } else if (Arrays.equals(MESSAGE.getRaw(), resp)) {
                final byte[] bchannel = (byte[]) reply.get(1);
                final byte[] bmesg = (byte[]) reply.get(2);
                final String strchannel = (bchannel == null) ? null : SafeEncoder.encode(bchannel);
                final String strmesg = (bmesg == null) ? null : SafeEncoder.encode(bmesg);
                onMessage(strchannel, strmesg);
            } else if (Arrays.equals(PMESSAGE.getRaw(), resp)) {
                final byte[] bpattern = (byte[]) reply.get(1);
                final byte[] bchannel = (byte[]) reply.get(2);
                final byte[] bmesg = (byte[]) reply.get(3);
                final String strpattern = (bpattern == null) ? null : SafeEncoder.encode(bpattern);
                final String strchannel = (bchannel == null) ? null : SafeEncoder.encode(bchannel);
                final String strmesg = (bmesg == null) ? null : SafeEncoder.encode(bmesg);
                onPMessage(strpattern, strchannel, strmesg);
            } else if (Arrays.equals(PSUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bpattern = (byte[]) reply.get(1);
                final String strpattern = (bpattern == null) ? null : SafeEncoder.encode(bpattern);
                onPSubscribe(strpattern, subscribedChannels);
            } else if (Arrays.equals(PUNSUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bpattern = (byte[]) reply.get(1);
                final String strpattern = (bpattern == null) ? null : SafeEncoder.encode(bpattern);
                onPUnsubscribe(strpattern, subscribedChannels);
            } else if (Arrays.equals(PONG.getRaw(), resp)) {
                final byte[] bpattern = (byte[]) reply.get(1);
                final String strpattern = (bpattern == null) ? null : SafeEncoder.encode(bpattern);
                onPong(strpattern);
            } else {
                throw new JedisException("Unknown message type: " + firstObj);
            }
        } while (isSubscribed());

        /* Invalidate instance since this thread is no longer listening */
        this.connection = null;
    }

    public int getSubscribedChannels() {
        return subscribedChannels;
    }
}