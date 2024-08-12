package redis.clients.jedis;

import static redis.clients.jedis.Protocol.ResponseKeyword.*;

import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public abstract class JedisPubSubBase<T> {

  private int subscribedChannels = 0;
  private volatile Connection client;

  public void onMessage(T channel, T message) {
  }

  public void onPMessage(T pattern, T channel, T message) {
  }

  public void onSubscribe(T channel, int subscribedChannels) {
  }

  public void onUnsubscribe(T channel, int subscribedChannels) {
  }

  public void onPUnsubscribe(T pattern, int subscribedChannels) {
  }

  public void onPSubscribe(T pattern, int subscribedChannels) {
  }

  public void onPong(T pattern) {
  }

  private void sendAndFlushCommand(Command command, T... args) {
    if (client == null) {
      throw new JedisException(getClass() + " is not connected to a Connection.");
    }
    CommandArguments cargs = new CommandArguments(command).addObjects(args);
    client.sendCommand(cargs);
    client.flush();
  }

  public final void unsubscribe() {
    sendAndFlushCommand(Command.UNSUBSCRIBE);
  }

  public final void unsubscribe(T... channels) {
    sendAndFlushCommand(Command.UNSUBSCRIBE, channels);
  }

  public final void subscribe(T... channels) {
    sendAndFlushCommand(Command.SUBSCRIBE, channels);
  }

  public final void psubscribe(T... patterns) {
    sendAndFlushCommand(Command.PSUBSCRIBE, patterns);
  }

  public final void punsubscribe() {
    sendAndFlushCommand(Command.PUNSUBSCRIBE);
  }

  public final void punsubscribe(T... patterns) {
    sendAndFlushCommand(Command.PUNSUBSCRIBE, patterns);
  }

  public final void ping() {
    sendAndFlushCommand(Command.PING);
  }

  public final void ping(T argument) {
    sendAndFlushCommand(Command.PING, argument);
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
      subscribe(channels);
      process();
    } finally {
      this.client.rollbackTimeout();
    }
  }

  public final void proceedWithPatterns(Connection client, T... patterns) {
    this.client = client;
    this.client.setTimeoutInfinite();
    try {
      psubscribe(patterns);
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
        if (Arrays.equals(SUBSCRIBE.getRaw(), resp)) {
          subscribedChannels = ((Long) listReply.get(2)).intValue();
          final byte[] bchannel = (byte[]) listReply.get(1);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          onSubscribe(enchannel, subscribedChannels);
        } else if (Arrays.equals(UNSUBSCRIBE.getRaw(), resp)) {
          subscribedChannels = ((Long) listReply.get(2)).intValue();
          final byte[] bchannel = (byte[]) listReply.get(1);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          onUnsubscribe(enchannel, subscribedChannels);
        } else if (Arrays.equals(MESSAGE.getRaw(), resp)) {
          final byte[] bchannel = (byte[]) listReply.get(1);
          final Object mesg = listReply.get(2);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          if (mesg instanceof List) {
            ((List<byte[]>) mesg).forEach(bmesg -> onMessage(enchannel, encode(bmesg)));
          } else {
            onMessage(enchannel, (mesg == null) ? null : encode((byte[]) mesg));
          }
        } else if (Arrays.equals(PMESSAGE.getRaw(), resp)) {
          final byte[] bpattern = (byte[]) listReply.get(1);
          final byte[] bchannel = (byte[]) listReply.get(2);
          final byte[] bmesg = (byte[]) listReply.get(3);
          final T enpattern = (bpattern == null) ? null : encode(bpattern);
          final T enchannel = (bchannel == null) ? null : encode(bchannel);
          final T enmesg = (bmesg == null) ? null : encode(bmesg);
          onPMessage(enpattern, enchannel, enmesg);
        } else if (Arrays.equals(PSUBSCRIBE.getRaw(), resp)) {
          subscribedChannels = ((Long) listReply.get(2)).intValue();
          final byte[] bpattern = (byte[]) listReply.get(1);
          final T enpattern = (bpattern == null) ? null : encode(bpattern);
          onPSubscribe(enpattern, subscribedChannels);
        } else if (Arrays.equals(PUNSUBSCRIBE.getRaw(), resp)) {
          subscribedChannels = ((Long) listReply.get(2)).intValue();
          final byte[] bpattern = (byte[]) listReply.get(1);
          final T enpattern = (bpattern == null) ? null : encode(bpattern);
          onPUnsubscribe(enpattern, subscribedChannels);
        } else if (Arrays.equals(PONG.getRaw(), resp)) {
          final byte[] bpattern = (byte[]) listReply.get(1);
          final T enpattern = (bpattern == null) ? null : encode(bpattern);
          onPong(enpattern);
        } else {
          throw new JedisException("Unknown message type: " + firstObj);
        }
      } else if (reply instanceof byte[]) {
        byte[] resp = (byte[]) reply;
        if ("PONG".equals(SafeEncoder.encode(resp))) {
          onPong(null);
        } else {
          onPong(encode(resp));
        }
      } else {
        throw new JedisException("Unknown message type: " + reply);
      }
    } while (!Thread.currentThread().isInterrupted() && isSubscribed());

//    /* Invalidate instance since this thread is no longer listening */
//    this.client = null;
  }
}
