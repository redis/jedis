package redis.clients.jedis.csc;

import java.lang.ref.WeakReference;
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
  private final ReentrantLock lock;

  public CacheConnection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig, Cache clientSideCache) {
    super(socketFactory, clientConfig);

    if (protocol != RedisProtocol.RESP3) {
      throw new JedisException("Client side caching is only supported with RESP3.");
    }
    this.clientSideCache = Objects.requireNonNull(clientSideCache);
    initializeClientSideCache();

    lock = new ReentrantLock();
  }

  @Override
  protected Object protocolRead(RedisInputStream inputStream) {
    if (lock != null) {
      lock.lock();
      try {
        return Protocol.read(inputStream);
      } finally {
        lock.unlock();
      }
    } else {
      return Protocol.read(inputStream);
    }
  }

  @Override
  protected void protocolReadPushes(RedisInputStream inputStream, boolean onlyPendingBuffer) {
    if (lock != null && lock.tryLock()) {
      try {
        Protocol.readPushes(inputStream, clientSideCache, onlyPendingBuffer);
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public <T> T executeCommand(final CommandObject<T> commandObject) {
    CacheKey key = new CacheKey<>(commandObject);
    if (!clientSideCache.isCacheable(key)) {
      return super.executeCommand(commandObject);
    }

    final CacheKey cacheKey = new CacheKey(commandObject);
    CacheEntry<T> cacheEntry = clientSideCache.get(cacheKey);

    // CACHE HIT !!
    if (cacheEntry != null) {
      cacheEntry = validateEntry(cacheEntry);
      if (cacheEntry != null) {
        return (T) cacheEntry.getValue();
      }
    }

    // CACHE MISS!!
    T value = super.executeCommand(commandObject);
    if (value != null) {
      cacheEntry = new CacheEntry<T>(cacheKey, value, new WeakReference(this));
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
    Connection cacheOwner = (Connection) cacheEntry.getConnection().get();
    if (cacheOwner == null) {
      clientSideCache.delete(cacheEntry.getCacheKey());
      return null;
    } else {
      if (cacheOwner == this) {
        this.readPushesWithCheckingBroken();
        cacheEntry = clientSideCache.get(cacheEntry.getCacheKey());
      }
    }
    return cacheEntry;
  }
}
