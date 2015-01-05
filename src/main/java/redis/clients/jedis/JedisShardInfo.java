package redis.clients.jedis;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.JedisURIHelper;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

public class JedisShardInfo extends ShardInfo<Jedis> {

  protected Logger log = Logger.getLogger(getClass().getName());
  
  public String toString() {
    return host + ":" + port + "*" + getWeight();
  }

  private int timeout;
  private String host;
  private int port;
  private String password = null;
  private String name = null;
  //Default Redis DB
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
    if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
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

  public JedisShardInfo(URI uri) {
    super(Sharded.DEFAULT_WEIGHT);
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
    Jedis jedis = new Jedis(this);
    try {
      jedis.select(db);
    } catch(JedisException e) {
      log.log(Level.SEVERE, "Can't select database due a Redis connection problem", e);
    }
      
    return jedis;
  }

}
