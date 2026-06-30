package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static redis.clients.jedis.Protocol.Command.PING;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

public class JedisMockTest {

  private TcpMockServer mockServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Test
  public void pingHandlesPubSubStylePongReply() {
    mockServer.setCommandHandler((commandArgs, clientId) -> {
      if (!PING.equals(commandArgs.getCommand())) {
        return null;
      }
      return RespResponse.array(RespResponse.bulkString("pong"), RespResponse.bulkString(""));
    });

    try (Jedis jedis = createJedis()) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void pingWithStringArgumentHandlesPubSubStylePongReply() {
    mockServer.setCommandHandler((commandArgs, clientId) -> {
      if (!PING.equals(commandArgs.getCommand())) {
        return null;
      }
      return RespResponse.array(RespResponse.bulkString("pong"),
        RespResponse.bulkString(commandArgs.get(1).getRaw()));
    });

    try (Jedis jedis = createJedis()) {
      assertEquals("hello", jedis.ping("hello"));
    }
  }

  @Test
  public void pingWithBinaryArgumentHandlesPubSubStylePongReply() {
    mockServer.setCommandHandler((commandArgs, clientId) -> {
      if (!PING.equals(commandArgs.getCommand())) {
        return null;
      }
      return RespResponse.array(RespResponse.bulkString("pong"),
        RespResponse.bulkString(commandArgs.get(1).getRaw()));
    });

    try (Jedis jedis = createJedis()) {
      assertArrayEquals(SafeEncoder.encode("hello"), jedis.ping(SafeEncoder.encode("hello")));
    }
  }

  private Jedis createJedis() {
    return new Jedis("localhost", mockServer.getPort(),
      DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP2).autoNegotiateProtocol(false).build());
  }
}
