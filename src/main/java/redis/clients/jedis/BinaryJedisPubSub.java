package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Keyword.MESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PMESSAGE;
import static redis.clients.jedis.Protocol.Keyword.PSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.PUNSUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.SUBSCRIBE;
import static redis.clients.jedis.Protocol.Keyword.UNSUBSCRIBE;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.exceptions.JedisException;

public abstract class BinaryJedisPubSub {
  private int subscribedChannels = 0;
  private Client client;

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

  public void unsubscribe() {
    client.unsubscribe();
    client.flush();
  }

  public void unsubscribe(byte[]... channels) {
    client.unsubscribe(channels);
    client.flush();
  }

  public void subscribe(byte[]... channels) {
    client.subscribe(channels);
    client.flush();
  }

  public void psubscribe(byte[]... patterns) {
    client.psubscribe(patterns);
    client.flush();
  }

  public void punsubscribe() {
    client.punsubscribe();
    client.flush();
  }

  public void punsubscribe(byte[]... patterns) {
    client.punsubscribe(patterns);
    client.flush();
  }

  public boolean isSubscribed() {
    return subscribedChannels > 0;
  }

  public void proceedWithPatterns(Client client, byte[]... patterns) {
    this.client = client;
    client.psubscribe(patterns);
    process(client);
  }

  public void proceed(Client client, byte[]... channels) {
    this.client = client;
    client.subscribe(channels);
    process(client);
  }

  private void process(Client client) {
    do {
      List<Object> reply = client.getObjectMultiBulkReply();
      final Object firstObj = reply.get(0);
      if (!(firstObj instanceof byte[])) {
        throw new JedisException("Unknown message type: " + firstObj);
      }
      final byte[] resp = (byte[]) firstObj;
      if (Arrays.equals(SUBSCRIBE.raw, resp)) {
        subscribedChannels = ((Long) reply.get(2)).intValue();
        final byte[] bchannel = (byte[]) reply.get(1);
        onSubscribe(bchannel, subscribedChannels);
      } else if (Arrays.equals(UNSUBSCRIBE.raw, resp)) {
        subscribedChannels = ((Long) reply.get(2)).intValue();
        final byte[] bchannel = (byte[]) reply.get(1);
        onUnsubscribe(bchannel, subscribedChannels);
      } else if (Arrays.equals(MESSAGE.raw, resp)) {
        final byte[] bchannel = (byte[]) reply.get(1);
        final byte[] bmesg = (byte[]) reply.get(2);
        onMessage(bchannel, bmesg);
      } else if (Arrays.equals(PMESSAGE.raw, resp)) {
        final byte[] bpattern = (byte[]) reply.get(1);
        final byte[] bchannel = (byte[]) reply.get(2);
        final byte[] bmesg = (byte[]) reply.get(3);
        onPMessage(bpattern, bchannel, bmesg);
      } else if (Arrays.equals(PSUBSCRIBE.raw, resp)) {
        subscribedChannels = ((Long) reply.get(2)).intValue();
        final byte[] bpattern = (byte[]) reply.get(1);
        onPSubscribe(bpattern, subscribedChannels);
      } else if (Arrays.equals(PUNSUBSCRIBE.raw, resp)) {
        subscribedChannels = ((Long) reply.get(2)).intValue();
        final byte[] bpattern = (byte[]) reply.get(1);
        onPUnsubscribe(bpattern, subscribedChannels);
      } else {
        throw new JedisException("Unknown message type: " + firstObj);
      }
    } while (isSubscribed());
  }

  public int getSubscribedChannels() {
    return subscribedChannels;
  }
}