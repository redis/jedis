package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import redis.clients.jedis.exceptions.JedisException;

/**
 * PoolableObjectFactory custom impl.
 */
class JedisFactory implements PooledObjectFactory<Jedis> {
  private final ClientOptions clientOptions;

  public JedisFactory(final ClientOptions clientOptions) {
    this.clientOptions = clientOptions;
  }

  @Override
  public void destroyObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final BinaryJedis jedis = pooledJedis.getObject();
    if (jedis.isConnected()) {
      try {
        try {
          jedis.quit();
        } catch (Exception e) {
        }
        jedis.disconnect();
      } catch (Exception e) {

      }
    }

  }

  @Override
  public PooledObject<Jedis> makeObject() throws Exception {
    final Jedis jedis = new Jedis(clientOptions);

    try {
      jedis.connect();
      if (clientOptions.getPassword() != null) {
        jedis.auth(clientOptions.getPassword());
      }
      if (clientOptions.getDatabase() != 0) {
        jedis.select(clientOptions.getDatabase());
      }
      if (clientOptions.getClientName() != null) {
        jedis.clientSetname(clientOptions.getClientName());
      }
    } catch (JedisException je) {
      jedis.close();
      throw je;
    }

    return new DefaultPooledObject<Jedis>(jedis);

  }

  @Override
  public void passivateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    // TODO maybe should select db 0? Not sure right now.
  }

  @Override
  public boolean validateObject(PooledObject<Jedis> pooledJedis) {
    final BinaryJedis jedis = pooledJedis.getObject();
      return jedis.isConnected() && jedis.ping().equals("PONG");
  }

  @Override
  public void activateObject(PooledObject<Jedis> p) throws Exception {
    final BinaryJedis jedis = p.getObject();
    if (jedis.getDB() != clientOptions.getDatabase()) {
      jedis.select(clientOptions.getDatabase());
    }
  }
}