package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class JedisSentinelSlaveFactory implements PooledObjectFactory<Jedis> {
  private static final Logger logger = LoggerFactory.getLogger(JedisSentinelSlaveFactory.class);
  private final String masterName;
  private final int retryTime = 5;

  private final AtomicReference<List<HostAndPort>> slavesHostAndPort = new AtomicReference<>();
  private final int connectionTimeout;
  private final int soTimeout;
  private final String password;
  private final int database;
  private final String clientName;
  private final boolean ssl;
  private final SSLSocketFactory sslSocketFactory;
  private SSLParameters sslParameters;
  private HostnameVerifier hostnameVerifier;

  public JedisSentinelSlaveFactory(final int connectionTimeout, final int soTimeout, final String password, final int database, final String clientName, final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier, String masterName) {
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.database = database;
    this.clientName = clientName;
    this.ssl = ssl;
    this.sslSocketFactory = sslSocketFactory;
    this.sslParameters = sslParameters;
    this.hostnameVerifier = hostnameVerifier;
    this.masterName = masterName;
  }

  public void setSlavesHostAndPort(final List<HostAndPort> slaveHostAndPort) {
    if (slaveHostAndPort == null || slaveHostAndPort.size() == 0) {
      return;
    }
    this.slavesHostAndPort.set(slaveHostAndPort);
  }

  @Override
  public void activateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final Jedis jedis = pooledJedis.getObject();
    if (jedis.getDB() != database) {
      jedis.select(database);
    }

  }

  @Override
  public void destroyObject(PooledObject<Jedis> pooledJedis) throws Exception {
    final Jedis jedis = pooledJedis.getObject();
    if (jedis.isConnected()) {
      try {
        try {
          jedis.close();
        } catch (Exception e) {
          logger.debug("Error while close", e);
        }
        jedis.disconnect();
      } catch (Exception e) {
        logger.debug("Error while disconnect", e);
      }
    }

  }

  @Override
  public PooledObject<Jedis> makeObject() throws Exception {
    final List<HostAndPort> slaves = slavesHostAndPort.get();
    if (slaves == null || slaves.isEmpty()) {
      throw new JedisException(String.format("No valid slave for master: %s,slave:%s", this.masterName, this.slavesHostAndPort));
    }
    DefaultPooledObject<Jedis> result = tryToGetSlave(slaves);
    if (null != result) {
      return result;
    } else {
      throw new JedisException(String.format("No valid slave for master: %s, after try %d times.", this.masterName, retryTime));
    }
  }

  private DefaultPooledObject<Jedis> tryToGetSlave(List<HostAndPort> slaves) {
    int retry = retryTime;
    while (retry >= 0) {
      retry--;
      int randomIndex = ThreadLocalRandom.current().nextInt(slaves.size());
      String host = slaves.get(randomIndex).getHost();
      int port = slaves.get(randomIndex).getPort();
      final Jedis jedisSlave = new Jedis(host, port, connectionTimeout, soTimeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
      try {
        jedisSlave.connect();
        if (null != this.password) {
          jedisSlave.auth(this.password);
        }
        if (database != 0) {
          jedisSlave.select(database);
        }
        if (clientName != null) {
          jedisSlave.clientSetname(clientName);
        }
        return new DefaultPooledObject<>(jedisSlave);

      } catch (Exception e) {
        jedisSlave.close();
        logger.error("tryToGetSlave error ", e);
      }
    }
    return null;
  }

  @Override
  public void passivateObject(PooledObject<Jedis> pooledJedis) throws Exception {
    // TODO maybe should select db 0? Not sure right now.
  }

  @Override
  public boolean validateObject(PooledObject<Jedis> pooledJedis) {
    final Jedis jedis = pooledJedis.getObject();
    try {
      return jedis.isConnected() && jedis.ping().equals("PONG");
    } catch (final Exception e) {
      return false;
    }
  }
}