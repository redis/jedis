package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

public abstract class JedisShardedPubSub extends JedisShardedPubSubBase<String> {

  @Override
  protected final String encode(byte[] raw) {
    return SafeEncoder.encode(raw);
  }
}
