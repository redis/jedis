package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Keyword.MESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PMESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PONG;
import static redis.clients.jedis.Protocol.Keyword.PSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.PUNSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.SUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.UNSUBSCRIBE;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.exceptions.JedisException;

public abstract class BinaryJedisPubSub {
    private int subscribedChannels = 0;
    private Connection connection;

    public void onMessage(byte[] channel, byte[] message) {
    }

    public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
    }

    public void onSubscribe(byte[] channel, int subscribedChannels) {
    }

    public void onUnsubscribe(byte[] channel, int subscribedChannels) {
    }

    public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
    }

    public void onPSubscribe(byte[] pattern, int subscribedChannels) {
    }

    public void onPong(byte[] pattern) {
    }

    public void unsubscribe() {
        connection.sendCommand(Protocol.Command.UNSUBSCRIBE);
        connection.flush();
    }

    public void unsubscribe(byte[]... channels) {
        connection.sendCommand(Protocol.Command.UNSUBSCRIBE, channels);
        connection.flush();
    }

    public void subscribe(byte[]... channels) {
        connection.sendCommand(Protocol.Command.SUBSCRIBE, channels);
        connection.flush();
    }

    public void psubscribe(byte[]... patterns) {
        connection.sendCommand(Protocol.Command.PSUBSCRIBE, patterns);
        connection.flush();
    }

    public void punsubscribe() {
        connection.sendCommand(Protocol.Command.PUNSUBSCRIBE);
        connection.flush();
    }

    public void punsubscribe(byte[]... patterns) {
        connection.sendCommand(Protocol.Command.PUNSUBSCRIBE, patterns);
        connection.flush();
    }

    public void ping() {
        connection.ping();
        connection.flush();
    }

    public void ping(byte[] argument) {
        connection.sendCommand(Protocol.Command.PING, argument);
        connection.flush();
    }

    public boolean isSubscribed() {
        return subscribedChannels > 0;
    }

    public void proceedWithPatterns(Connection connection, byte[]... patterns) {
        this.connection = connection;
        connection.sendCommand(Protocol.Command.PSUBSCRIBE, patterns);
        connection.flush();
        process(connection);
    }

    public void proceed(Connection connection, byte[]... channels) {
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
                onSubscribe(bchannel, subscribedChannels);
            } else if (Arrays.equals(UNSUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bchannel = (byte[]) reply.get(1);
                onUnsubscribe(bchannel, subscribedChannels);
            } else if (Arrays.equals(MESSAGE.getRaw(), resp)) {
                final byte[] bchannel = (byte[]) reply.get(1);
                final byte[] bmesg = (byte[]) reply.get(2);
                onMessage(bchannel, bmesg);
            } else if (Arrays.equals(PMESSAGE.getRaw(), resp)) {
                final byte[] bpattern = (byte[]) reply.get(1);
                final byte[] bchannel = (byte[]) reply.get(2);
                final byte[] bmesg = (byte[]) reply.get(3);
                onPMessage(bpattern, bchannel, bmesg);
            } else if (Arrays.equals(PSUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bpattern = (byte[]) reply.get(1);
                onPSubscribe(bpattern, subscribedChannels);
            } else if (Arrays.equals(PUNSUBSCRIBE.getRaw(), resp)) {
                subscribedChannels = ((Long) reply.get(2)).intValue();
                final byte[] bpattern = (byte[]) reply.get(1);
                onPUnsubscribe(bpattern, subscribedChannels);
            } else if (Arrays.equals(PONG.getRaw(), resp)) {
                final byte[] bpattern = (byte[]) reply.get(1);
                onPong(bpattern);
            } else {
                throw new JedisException("Unknown message type: " + firstObj);
            }
        } while (isSubscribed());
    }

    public int getSubscribedChannels() {
        return subscribedChannels;
    }
}