package redis.clients.jedis.tests;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import org.junit.Test;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSocketFactory;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;

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
    public Socket createSocket() throws JedisConnectionException {
      try {
        Socket socket = AFUNIXSocket.newStrictInstance();
        socket.connect(new AFUNIXSocketAddress(UDS_SOCKET), Protocol.DEFAULT_TIMEOUT);
        return socket;
      } catch (IOException ioe) {
        throw new JedisConnectionException("Failed to create UDS connection.", ioe);
      }
    }

    @Override
    public void updateHostAndPort(HostAndPort hostAndPort) {
      throw new UnsupportedOperationException("UDS cannot update host and port");
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
    public int getPort() {
      return 0;
    }

    @Override
    public int getConnectionTimeout() {
      return Protocol.DEFAULT_TIMEOUT;
    }

    @Override
    public int getSoTimeout() {
      return Protocol.DEFAULT_TIMEOUT;
    }
  }
}