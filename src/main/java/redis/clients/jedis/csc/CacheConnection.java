package redis.clients.jedis.csc;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSocketFactory;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.RedisInputStream;

public class CacheConnection extends Connection {

  private final ClientSideCache clientSideCache;
  private final ReentrantLock lock;

  public CacheConnection(final JedisSocketFactory socketFactory, JedisClientConfig clientConfig,
      ClientSideCache clientSideCache) {

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
  protected void protocolReadPushes(RedisInputStream inputStream) {
    if (lock != null && lock.tryLock()) {
      try {
        super.setSoTimeout(1);
        Protocol.readPushes(inputStream, clientSideCache);
      } finally {
        super.rollbackTimeout();
        lock.unlock();
      }
    }
  }

  private void initializeClientSideCache() {
    sendCommand(Protocol.Command.CLIENT, "TRACKING", "ON");
    String reply = getStatusCodeReply();
    if (!"OK".equals(reply)) {
      throw new JedisException("Could not enable client tracking. Reply: " + reply);
    }
  }
}
