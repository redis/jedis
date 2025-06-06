package redis.clients.jedis;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.TestEnvUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class UdsTest {

  @BeforeAll
  public static void checkDockerEnvironment() {
    assumeFalse(TestEnvUtil.isContainerEnv(),"Unix sockets tests not supported against dockerised test env yet!");
  }

  @Test
  public void jedisConnectsToUds() {
    try (Jedis jedis = new Jedis(new UdsJedisSocketFactory())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void jedisConnectsToUdsResp3() {
    try (Jedis jedis = new Jedis(new UdsJedisSocketFactory(),
        DefaultJedisClientConfig.builder().resp3().build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void unifiedJedisConnectsToUds() {
    try (UnifiedJedis jedis = new UnifiedJedis(new UdsJedisSocketFactory())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void unifiedJedisConnectsToUdsResp3() {
    try (UnifiedJedis jedis = new UnifiedJedis(new UdsJedisSocketFactory(),
        DefaultJedisClientConfig.builder().resp3().build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  private static class UdsJedisSocketFactory implements JedisSocketFactory {

    private static final File UDS_SOCKET = new File("/tmp/redis_uds.sock");

    @Override
    public Socket createSocket() throws JedisConnectionException {
      try {
        Socket socket = AFUNIXSocket.newStrictInstance();
        socket.connect(new AFUNIXSocketAddress(UDS_SOCKET), Protocol.DEFAULT_TIMEOUT);
        return socket;
      } catch (IOException ioe) {
        throw new JedisConnectionException("Failed to create UDS connection.", ioe);
      }
    }
  }
}