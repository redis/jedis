package redis.clients.jedis;

import java.net.URI;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.util.JedisURIHelper;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

public class JedisShardInfo extends ShardInfo<Jedis> {
  
  private int connectionTimeout;
  private int soTimeout;
  private String host;
  private int port;
  private String password = null;
  private String name = null;
  // Default Redis DB
  private int db = 0;

  public JedisShardInfo(String host) {
    super(Sharded.DEFAULT_WEIGHT);
    URI uri = URI.create(host);
    if (JedisURIHelper.isValid(uri)) {
      this.host = uri.getHost();
      this.port = uri.getPort();
      this.password = JedisURIHelper.getPassword(uri);
      this.db = JedisURIHelper.getDBIndex(uri);
    } else {
      this.host = host;
      this.port = Protocol.DEFAULT_PORT;
    }
  }

  public JedisShardInfo(String host, String name) {
    this(host, Protocol.DEFAULT_PORT, name);
  }

  public JedisShardInfo(String host, int port) {
    this(host, port, 2000);
  }

  public JedisShardInfo(String host, int port, String name) {
    this(host, port, 2000, name);
  }

  public JedisShardInfo(String host, int port, int timeout) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT);
  }

  public JedisShardInfo(String host, int port, int timeout, String name) {
    this(host, port, timeout, timeout, Sharded.DEFAULT_WEIGHT);
    this.name = name;
  }

  public JedisShardInfo(String host, int port, int connectionTimeout, int soTimeout, int weight) {
    super(weight);
    this.host = host;
    this.port = port;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
  }

  public JedisShardInfo(String host, String name, int port, int timeout, int weight) {
    super(weight);
    this.host = host;
    this.name = name;
    this.port = port;
    this.connectionTimeout = timeout;
    this.soTimeout = timeout;
  }

  public JedisShardInfo(URI uri) {
    super(Sharded.DEFAULT_WEIGHT);
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }

    this.host = uri.getHost();
    this.port = uri.getPort();
    this.password = JedisURIHelper.getPassword(uri);
    this.db = JedisURIHelper.getDBIndex(uri);
  }

  public String toString() {
    return host + ":" + port + "*" + getWeight();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String auth) {
    this.password = auth;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getSoTimeout() {
    return soTimeout;
  }

  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
  }

  public String getName() {
    return name;
  }

  public int getDb() {
    return db;
  }

  @Override
  public Jedis createResource() {
    return new Jedis(this);
  }
  
}
