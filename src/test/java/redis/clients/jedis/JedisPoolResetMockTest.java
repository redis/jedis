package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * RESET drops the connection back to the default user and database 0, so a pooled connection that
 * ran it no longer matches the pool's configuration and must not reach the next borrower.
 */
public class JedisPoolResetMockTest {

  private final Map<String, String> userByClient = new ConcurrentHashMap<>();
  private final Map<String, Integer> dbByClient = new ConcurrentHashMap<>();

  private TcpMockServer server;
  private HostAndPort hostAndPort;

  @BeforeEach
  public void setUp() throws IOException {
    server = new TcpMockServer();
    server.setCommandHandler((args, clientId) -> {
      switch (SafeEncoder.encode(args.getCommand().getRaw())) {
        case "HELLO":
          for (int i = 1; i + 2 < args.size(); i++) {
            if ("AUTH".equalsIgnoreCase(SafeEncoder.encode(args.get(i).getRaw()))) {
              userByClient.put(clientId, SafeEncoder.encode(args.get(i + 1).getRaw()));
            }
          }
          return null;
        case "AUTH":
          userByClient.put(clientId, SafeEncoder.encode(args.get(1).getRaw()));
          return RespResponse.ok();
        case "SELECT":
          dbByClient.put(clientId, Integer.parseInt(SafeEncoder.encode(args.get(1).getRaw())));
          return RespResponse.ok();
        case "RESET":
          userByClient.put(clientId, "default");
          dbByClient.put(clientId, 0);
          return RespResponse.simpleString("RESET");
        case "GET":
          return RespResponse.bulkString("user=" + userByClient.getOrDefault(clientId, "default")
              + " db=" + dbByClient.getOrDefault(clientId, 0));
        default:
          return null;
      }
    });
    server.start();
    hostAndPort = new HostAndPort("localhost", server.getPort());
  }

  @AfterEach
  public void tearDown() throws IOException {
    server.stop();
  }

  @Test
  public void resetConnectionIsNotHandedToNextBorrower() {
    GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    JedisClientConfig config = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP2)
        .user("app").password("secret").database(2).build();

    try (JedisPool pool = new JedisPool(poolConfig, hostAndPort, config)) {
      try (Jedis jedis = pool.getResource()) {
        assertEquals("user=app db=2", jedis.get("whoami"));
        assertEquals("RESET", jedis.reset());
      }

      try (Jedis jedis = pool.getResource()) {
        assertEquals("user=app db=2", jedis.get("whoami"));
      }
      assertEquals(1, pool.getDestroyedCount());
    }
  }
}
