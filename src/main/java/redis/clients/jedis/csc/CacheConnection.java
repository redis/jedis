package redis.clients.jedis.csc;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSocketFactory;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.RedisInputStream;

public class CacheConnection extends Connection {

  private final Cache cache;
  private ReentrantLock lock;
  private static final String REDIS = "redis";
  private static final String MIN_REDIS_VERSION = "7.4";

  public CacheConnection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig, Cache cache) {
    super(socketFactory, clientConfig);

    if (protocol != RedisProtocol.RESP3) {
      throw new JedisException("Client side caching is only supported with RESP3.");
    }
    if (!cache.compatibilityMode()) {
      RedisVersion current = new RedisVersion(version);
      RedisVersion required = new RedisVersion(MIN_REDIS_VERSION);
      if (!REDIS.equals(server) || current.compareTo(required) < 0) {
        throw new JedisException(String.format("Client side caching is only supported with 'Redis %s' or later.", MIN_REDIS_VERSION));
      }
    }
    this.cache = Objects.requireNonNull(cache);
    initializeClientSideCache();
  }

  @Override
  protected void initializeFromClientConfig(JedisClientConfig config) {
    lock = new ReentrantLock();
    super.initializeFromClientConfig(config);
  }

  @Override
  protected Object protocolRead(RedisInputStream inputStream) {
    lock.lock();
    try {
      return Protocol.read(inputStream, cache);
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected void protocolReadPushes(RedisInputStream inputStream) {
    if (lock.tryLock()) {
      try {
        Protocol.readPushes(inputStream, cache, true);
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public void disconnect() {
    super.disconnect();
    cache.flush();
  }

  @Override
  public <T> T executeCommand(final CommandObject<T> commandObject) {
    final CacheKey cacheKey = new CacheKey(commandObject);
    if (!cache.isCacheable(cacheKey)) {
      cache.getStats().nonCacheable();
      return super.executeCommand(commandObject);
    }

    CacheEntry<T> cacheEntry = cache.get(cacheKey);
    if (cacheEntry != null) { // (probable) CACHE HIT !!
      cacheEntry = validateEntry(cacheEntry);
      if (cacheEntry != null) {
        // CACHE HIT confirmed !!!
        cache.getStats().hit();
        return cacheEntry.getValue();
      }
    }

    // CACHE MISS !!
    cache.getStats().miss();
    T value = super.executeCommand(commandObject);
    cacheEntry = new CacheEntry<>(cacheKey, value, this);
    cache.set(cacheKey, cacheEntry);
    // this line actually provides a deep copy of cached object instance 
    value = cacheEntry.getValue();
    return value;
  }

  public Cache getCache() {
    return cache;
  }

  private void initializeClientSideCache() {
    sendCommand(Protocol.Command.CLIENT, "TRACKING", "ON");
    String reply = getStatusCodeReply();
    if (!"OK".equals(reply)) {
      throw new JedisException("Could not enable client tracking. Reply: " + reply);
    }
  }

  private CacheEntry validateEntry(CacheEntry cacheEntry) {
    CacheConnection cacheOwner = cacheEntry.getConnection();
    if (cacheOwner == null || cacheOwner.isBroken() || !cacheOwner.isConnected()) {
      cache.delete(cacheEntry.getCacheKey());
      return null;
    } else {
      try {
        cacheOwner.readPushesWithCheckingBroken();
      } catch (JedisException e) {
        cache.delete(cacheEntry.getCacheKey());
        return null;
      }

      return cache.get(cacheEntry.getCacheKey());
    }
  }
}
