package redis.clients.jedis;

import java.net.URI;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.util.JedisURIHelper;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

public class JedisShardInfo extends ShardInfo<Jedis> {

  public String toString() {
    return host + ":" + port + "*" + getWeight();
  }

  private int timeout;
  private String host;
  private int port;
  private String password = null;
  private String name = null;
  // Default Redis DB
  private int db = 0;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

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
    this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
  }

  public JedisShardInfo(String host, int port, int timeout, String name) {
    this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
    this.name = name;
  }

  public JedisShardInfo(String host, int port, int timeout, int weight) {
    super(weight);
    this.host = host;
    this.port = port;
    this.timeout = timeout;
  }

  public JedisShardInfo(String host, String name, int port, int timeout, int weight) {
    super(weight);
    this.host = host;
    this.name = name;
    this.port = port;
    this.timeout = timeout;
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String auth) {
    this.password = auth;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
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
