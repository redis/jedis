package redis.clients.jedis;

import static redis.clients.jedis.Protocol.ResponseKeyword.*;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisException;

public abstract class JedisShardedPubSubBase<T> {

  private int subscribedChannels = 0;
  private volatile Connection client;

  public void onSMessage(T channel, T message) {
  }

  public void onSSubscribe(T channel, int subscribedChannels) {
  }

  public void onSUnsubscribe(T channel, int subscribedChannels) {
  }

  private void sendAndFlushCommand(Command command, T... args) {
    if (client == null) {
      throw new JedisException(getClass() + " is not connected to a Connection.");
    }
    CommandArguments cargs = new CommandArguments(command).addObjects(args);
    client.sendCommand(cargs);
    client.flush();
  }

  public final void sunsubscribe() {
    sendAndFlushCommand(Command.SUNSUBSCRIBE);
  }

  public final void sunsubscribe(T... channels) {
    sendAndFlushCommand(Command.SUNSUBSCRIBE, channels);
  }

  public final void ssubscribe(T... channels) {
    sendAndFlushCommand(Command.SSUBSCRIBE, channels);
  }

  public final boolean isSubscribed() {
    return subscribedChannels > 0;
  }

  public final int getSubscribedChannels() {
    return subscribedChannels;
  }

  public final void proceed(Connection client, T... channels) {
    this.client = client;
    this.client.setTimeoutInfinite();
    try {
      ssubscribe(channels);
      process();
    } finally {
      this.client.rollbackTimeout();
    }
  }

  protected abstract T encode(byte[] raw);

//  private void process(Client client) {
  private void process() {

    do {
      Object reply = client.getUnflushedObject();

      if (reply instanceof List) {
        List<Object> listReply = (List<Object>) reply;
        final Object firstObj = listReply.get(0);
        if (!(firstObj instanceof byte[])) {
          throw new JedisException("Unknown message type: " + firstObj);
        }
        final byte[] resp = (byte[]) firstObj;
        if (Arrays.equals(SSUBSCRIBE.getRaw(), resp)) {
          subscribedChannels = ((Long) listReply.get(2)).intValue();
          final byte[] bchannel = (byte[]) listReply.get(1);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          onSSubscribe(enchannel, subscribedChannels);
        } else if (Arrays.equals(SUNSUBSCRIBE.getRaw(), resp)) {
          subscribedChannels = ((Long) listReply.get(2)).intValue();
          final byte[] bchannel = (byte[]) listReply.get(1);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          onSUnsubscribe(enchannel, subscribedChannels);
        } else if (Arrays.equals(SMESSAGE.getRaw(), resp)) {
          final byte[] bchannel = (byte[]) listReply.get(1);
          final byte[] bmesg = (byte[]) listReply.get(2);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          final T enmesg = (bmesg == null) ? null : encode(bmesg);
          onSMessage(enchannel, enmesg);
        } else {
          throw new JedisException("Unknown message type: " + firstObj);
        }
      } else {
        throw new JedisException("Unknown message type: " + reply);
      }
    } while (!Thread.currentThread().isInterrupted() && isSubscribed());

//    /* Invalidate instance since this thread is no longer listening */
//    this.client = null;
  }
}
