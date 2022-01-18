package redis.clients.jedis;

import static redis.clients.jedis.Protocol.ResponseKeyword.*;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisException;

public abstract class BinaryJedisPubSub {
  private int subscribedChannels = 0;
  private Connection client;

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
    client.sendCommand(Command.UNSUBSCRIBE);
    client.flush();
  }

  public void unsubscribe(byte[]... channels) {
    client.sendCommand(Command.UNSUBSCRIBE, channels);
    client.flush();
  }

  public void subscribe(byte[]... channels) {
    client.sendCommand(Command.SUBSCRIBE, channels);
    client.flush();
  }

  public void psubscribe(byte[]... patterns) {
    client.sendCommand(Command.PSUBSCRIBE, patterns);
    client.flush();
  }

  public void punsubscribe() {
    client.sendCommand(Command.PUNSUBSCRIBE);
    client.flush();
  }

  public void punsubscribe(byte[]... patterns) {
    client.sendCommand(Command.PUNSUBSCRIBE, patterns);
    client.flush();
  }

  public void ping() {
    client.sendCommand(Command.PING);
    client.flush();
  }

  public void ping(byte[] argument) {
    client.sendCommand(Command.PING, argument);
    client.flush();
  }

  public boolean isSubscribed() {
    return subscribedChannels > 0;
  }

  public void proceedWithPatterns(Connection client, byte[]... patterns) {
    this.client = client;
    this.client.setTimeoutInfinite();
    try {
      psubscribe(patterns);
      process();
    } finally {
      this.client.rollbackTimeout();
    }
  }

  public void proceed(Connection client, byte[]... channels) {
    this.client = client;
    this.client.setTimeoutInfinite();
    try {
      subscribe(channels);
      process();
    } finally {
      this.client.rollbackTimeout();
    }
  }

  private void process() {
    do {
      List<Object> reply = client.getUnflushedObjectMultiBulkReply();
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
