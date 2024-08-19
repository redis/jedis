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

  public CacheConnection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig,
      Cache clientSideCache) {
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
    final CacheKey<T> cacheKey = new CacheKey<>(commandObject);

    // Check if key is cachable at all, if not just execute the command as if there is no cache enabled
    if (!clientSideCache.isCacheable(cacheKey)) {
      clientSideCache.getStats().nonCacheable();
      return super.executeCommand(commandObject);
    }

    // Check if the cache contains an entry for the provided key
    CacheEntry<T> cacheEntry = clientSideCache.get(cacheKey);
    cacheEntry = validateEntry(cacheEntry);
    if (cacheEntry != null) {
      // We only return the key if it is valid, otherwise we follow the flow if there was no cache hit
      clientSideCache.getStats().hit();
      return (T) cacheEntry.getValue();
    }

    // ---
    // At this point we know there is no valid cache entry to return,
    // so we attempt to pull one from the server and cache it
    // ---

    clientSideCache.getStats().miss();
    cacheEntry = CacheEntry.inProgress(cacheKey, this);
    clientSideCache.set(cacheKey, cacheEntry);

    T value = super.executeCommand(commandObject);
    cacheEntry = clientSideCache.get(cacheKey);
    if (shouldCacheEntry(value, cacheEntry)) {
      cacheEntry = CacheEntry.newCacheEntry(cacheKey, this, value);
      clientSideCache.set(cacheKey, cacheEntry);
      clientSideCache.getStats().load();

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

  private <T> boolean shouldCacheEntry(T value, CacheEntry cacheEntry) {

    if (cacheEntry != null) {
      if (value != null && cacheEntry.inProgress()) {
        // TODO shouldn't we cache null values?
        return true;
      }

      clientSideCache.delete(cacheEntry.getCacheKey());
    }

    // if cache entry was null or is not in progress then either it was invalidated before we were able to get
    // the data or some other thread has managed to update it before us, we should assume the value is no longer valid
    return false;
  }

  private CacheEntry validateEntry(CacheEntry cacheEntry) {

    if (cacheEntry == null) {
      return null;
    }

    CacheConnection cacheOwner = cacheEntry.getConnection();
    try {
      if (cacheOwner != null && !cacheOwner.isBroken() && cacheOwner.isConnected()) {
        cacheOwner.readPushesWithCheckingBroken();
        CacheEntry updated = clientSideCache.get(cacheEntry.getCacheKey());
        if (updated != null && !updated.inProgress()) {
          return updated;
        }
      }

    } catch (JedisException e) {
      // continue below to invalidate the key, no different action required
    }

    // in any of the cases above we need to delete the cache entry and disregard the cached result
    // the logic should be able to recover by assuming this is not a cache hit, but a cache miss
    clientSideCache.getStats().invalidationByCheck();
    clientSideCache.delete(cacheEntry.getCacheKey());
    return null;
  }

}
