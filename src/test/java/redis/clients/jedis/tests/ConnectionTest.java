package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Test;

import redis.clients.jedis.JedisConnection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class ConnectionTest {

  private JedisConnection client;

  @After
  public void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
  }

  @Test(expected = JedisConnectionException.class)
  public void checkUnkownHost() {
    client = new JedisConnection("someunknownhost", Protocol.DEFAULT_PORT);
    client.connect();
  }

  @Test(expected = JedisConnectionException.class)
  public void checkWrongPort() {
    client = new JedisConnection(Protocol.DEFAULT_HOST, 55665);
    client.connect();
  }

  @Test
  public void connectIfNotConnectedWhenSettingTimeoutInfinite() {
    client = new JedisConnection("localhost", 6379);
    client.setTimeoutInfinite();
  }

  @Test
  public void checkCloseable() {
    client = new JedisConnection("localhost", 6379);
    client.connect();
    client.close();
  }
}
