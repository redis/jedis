package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisException;

public class JedisClientSideCache extends Jedis {

  private final ClientSideCache cache;

  public JedisClientSideCache(final HostAndPort hostPort, final JedisClientConfig config) {
    this(hostPort, config, new ClientSideCache());
  }

  public JedisClientSideCache(final HostAndPort hostPort, final JedisClientConfig config,
      ClientSideCache cache) {
    super(hostPort, config);
    if (config.getRedisProtocol() != RedisProtocol.RESP3) {
      throw new JedisException("Client side caching is only supported with RESP3.");
    }

    this.cache = cache;
    this.connection.setClientSideCache(cache);
    clientTrackingOn();
  }

  private void clientTrackingOn() {
    String reply = connection.executeCommand(new CommandObject<>(
        new CommandArguments(Protocol.Command.CLIENT).add("TRACKING").add("ON"),
        BuilderFactory.STRING));
    if (!"OK".equals(reply)) {
      throw new JedisException("Could not enable client tracking. Reply: " + reply);
    }
  }

  @Override
  public String get(String key) {
    //connection.readPushesWithCheckingBroken();
    String cachedValue = cache.getValue(key);
    if (cachedValue != null) return cachedValue;

    String value = super.get(key);
    if (value != null) cache.setKey(key, value);
    return value;
  }

}
