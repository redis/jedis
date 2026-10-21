package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * A pooled {@link Jedis} must come back on the pool's configured database, whichever API changed
 * it. The mock server tracks the database selected on each connection and answers GET with it.
 */
public class JedisPoolSelectedDatabaseMockTest {

  private TcpMockServer mockServer;
  private final Map<String, String> selectedDb = new ConcurrentHashMap<>();

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.setCommandHandler((args, clientId) -> {
      String cmd = SafeEncoder.encode(args.getCommand().getRaw());
      if ("SELECT".equalsIgnoreCase(cmd)) {
        selectedDb.put(clientId, SafeEncoder.encode(args.get(1).getRaw()));
        return "+OK\r\n";
      }
      if ("GET".equalsIgnoreCase(cmd)) {
        String db = selectedDb.getOrDefault(clientId, "0");
        return "$" + db.length() + "\r\n" + db + "\r\n";
      }
      return null;
    });
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Test
  public void pipelinedSelectDoesNotLeakToNextBorrower() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(1);

    try (JedisPool pool = new JedisPool(poolConfig,
        new HostAndPort("localhost", mockServer.getPort()),
        DefaultJedisClientConfig.builder().build())) {
      try (Jedis jedis = pool.getResource()) {
        Pipeline p = jedis.pipelined();
        p.select(1);
        p.sync();
        assertEquals(1, jedis.getDB());
      }

      try (Jedis jedis = pool.getResource()) {
        assertEquals("0", jedis.get("key"));
      }
    }
  }
}
