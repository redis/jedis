package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

public abstract class JedisPubSub extends JedisPubSubBase<String> {

  @Override
  protected final String encode(byte[] raw) {
    return SafeEncoder.encode(raw);
  }
}
