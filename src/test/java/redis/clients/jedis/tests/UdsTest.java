package redis.clients.jedis.tests;

import org.junit.Test;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSocketFactory;
import redis.clients.jedis.Protocol;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class UdsTest {

  @Test
  public void testConnectsToUds() {
    try (Jedis jedis = new Jedis(new UdsJedisSocketFactory())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  private static class UdsJedisSocketFactory implements JedisSocketFactory {

    private static final File UDS_SOCKET = new File("/tmp/redis_uds.sock");

    @Override
    public Socket createSocket() throws IOException {
      Socket socket = AFUNIXSocket.newStrictInstance();
      socket.connect(new AFUNIXSocketAddress(UDS_SOCKET), Protocol.DEFAULT_TIMEOUT);
      return socket;
    }

    @Override
    public String getDescription() {
      return UDS_SOCKET.toString();
    }

    @Override
    public String getHost() {
      return UDS_SOCKET.toString();
    }

    @Override
    public void setHost(String host) {
    }

    @Override
    public int getPort() {
      return 0;
    }

    @Override
    public void setPort(int port) {
    }

    @Override
    public int getConnectionTimeout() {
      return Protocol.DEFAULT_TIMEOUT;
    }

    @Override
    public void setConnectionTimeout(int connectionTimeout) {
    }

    @Override
    public int getSoTimeout() {
      return Protocol.DEFAULT_TIMEOUT;
    }

    @Override
    public void setSoTimeout(int soTimeout) {
    }
  }
}