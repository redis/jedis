package redis.clients.jedis;

import static redis.clients.jedis.Protocol.ResponseKeyword.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public abstract class JedisPubSubBase<T> {

  private int subscribedChannels = 0;
  private final JedisSafeAuthenticator authenticator = new JedisSafeAuthenticator();
  private final Consumer<Object> pingResultHandler = this::processPingReply;

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
    authenticator.sendAndFlushCommand(command, args);
  }

  public final void unsubscribe() {
    sendAndFlushCommand(Command.UNSUBSCRIBE);
  }

  public final void unsubscribe(T... channels) {
    sendAndFlushCommand(Command.UNSUBSCRIBE, channels);
  }

  public final void subscribe(T... channels) {
    checkConnectionSuitableForPubSub();
    sendAndFlushCommand(Command.SUBSCRIBE, channels);
  }

  public final void psubscribe(T... patterns) {
    checkConnectionSuitableForPubSub();
    sendAndFlushCommand(Command.PSUBSCRIBE, patterns);
  }

  private void checkConnectionSuitableForPubSub() {
    if (authenticator.client.protocol != RedisProtocol.RESP3
        && authenticator.client.isTokenBasedAuthenticationEnabled()) {
      throw new JedisException(
          "Blocking pub/sub operations are not supported on token-based authentication enabled connections with RESP2 protocol!");
    }
  }

  public final void punsubscribe() {
    sendAndFlushCommand(Command.PUNSUBSCRIBE);
  }

  public final void punsubscribe(T... patterns) {
    sendAndFlushCommand(Command.PUNSUBSCRIBE, patterns);
  }

  public final void ping() {
    authenticator.commandSync.lock();
    try {
      sendAndFlushCommand(Command.PING);
      authenticator.resultHandler.add(pingResultHandler);
    } finally {
      authenticator.commandSync.unlock();
    }
  }

  public final void ping(T argument) {
    authenticator.commandSync.lock();
    try {
      sendAndFlushCommand(Command.PING, argument);
      authenticator.resultHandler.add(pingResultHandler);
    } finally {
      authenticator.commandSync.unlock();
    }
  }

  public final boolean isSubscribed() {
    return subscribedChannels > 0;
  }

  public final int getSubscribedChannels() {
    return subscribedChannels;
  }

  public final void proceed(Connection client, T... channels) {
    authenticator.registerForAuthentication(client);
    authenticator.client.setTimeoutInfinite();
    try {
      subscribe(channels);
      process();
    } finally {
      authenticator.client.rollbackTimeout();
    }
  }

  public final void proceedWithPatterns(Connection client, T... patterns) {
    authenticator.registerForAuthentication(client);
    authenticator.client.setTimeoutInfinite();
    try {
      psubscribe(patterns);
      process();
    } finally {
      authenticator.client.rollbackTimeout();
    }
  }

  protected abstract T encode(byte[] raw);

  //  private void process(Client client) {
  private void process() {

    do {
      Object reply = authenticator.client.getUnflushedObject();

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
        Consumer<Object> resultHandler = authenticator.resultHandler.poll();
        if (resultHandler == null) {
          throw new JedisException("Unexpected message : " + SafeEncoder.encode((byte[]) reply));
        }
        resultHandler.accept(reply);
      } else {
        throw new JedisException("Unknown message type: " + reply);
      }
    } while (!Thread.currentThread().isInterrupted() && isSubscribed());

    //    /* Invalidate instance since this thread is no longer listening */
    //    this.client = null;
  }

  private void processPingReply(Object reply) {
    byte[] resp = (byte[]) reply;
    if ("PONG".equals(SafeEncoder.encode(resp))) {
      onPong(null);
    } else {
      onPong(encode(resp));
    }
  }
}
