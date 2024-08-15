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

  private final Cache clientSideCache;
  private ReentrantLock lock;

  public CacheConnection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig, Cache clientSideCache) {
    super(socketFactory, clientConfig);

    if (protocol != RedisProtocol.RESP3) {
      throw new JedisException("Client side caching is only supported with RESP3.");
    }
    this.clientSideCache = Objects.requireNonNull(clientSideCache);
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
      return Protocol.read(inputStream, clientSideCache);
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected void protocolReadPushes(RedisInputStream inputStream) {
    if (lock.tryLock()) {
      try {
        Protocol.readPushes(inputStream, clientSideCache, true);
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public void disconnect() {
    super.disconnect();
    clientSideCache.flush();
  }

  @Override
  public <T> T executeCommand(final CommandObject<T> commandObject) {
    CacheKey key = new CacheKey<>(commandObject);
    if (!clientSideCache.isCacheable(key)) {
      clientSideCache.getStats().nonCacheable();
      return super.executeCommand(commandObject);
    }

    final CacheKey cacheKey = new CacheKey(commandObject);
    CacheEntry<T> cacheEntry = clientSideCache.get(cacheKey);

    // CACHE HIT !!
    if (cacheEntry != null) {
      cacheEntry = validateEntry(cacheEntry);
      if (cacheEntry != null) {
        clientSideCache.getStats().hit();
        return (T) cacheEntry.getValue();
      }
    }

    // CACHE MISS!!
    clientSideCache.getStats().miss();
    T value = super.executeCommand(commandObject);
    if (value != null) {
      cacheEntry = new CacheEntry<>(cacheKey, value, this);
      clientSideCache.set(cacheKey, cacheEntry);
      // this line actually provides a deep copy of cached object instance 
      value = cacheEntry.getValue();
    }
    return value;
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
      clientSideCache.delete(cacheEntry.getCacheKey());
      return null;
    } else {
      try {
        cacheOwner.readPushesWithCheckingBroken();
      } catch (JedisException e) {
        clientSideCache.delete(cacheEntry.getCacheKey());
        return null;
      }

      return clientSideCache.get(cacheEntry.getCacheKey());
    }
  }
}
