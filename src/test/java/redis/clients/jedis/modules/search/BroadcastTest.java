package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import java.util.Map;
import java.util.function.Supplier;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisBroadcast;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.schemafields.TextField;

public class BroadcastTest extends RedisModuleCommandsTestBase {

  private static final String index = "broadcast";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  @Test
  public void broadcast() throws Exception {
    final Connection conn = new Connection(hnp);
    try (ConnectionProvider provider = new ConnectionProvider() {
      @Override
      public Connection getConnection() {
        return conn;
      }

      @Override
      public Connection getConnection(CommandArguments args) {
        return getConnection();
      }

      @Override
      public void close() throws Exception {
        conn.close();
      }
    }) {
      JedisBroadcast broadcast = new JedisBroadcast(provider);
      Map<?, Supplier<String>> reply = broadcast.ftCreateBroadcast(index, TextField.of("t"));
      assertEquals(1, reply.size());
      assertOK(reply.values().stream().findAny().get().get());
    }
    assertFalse(conn.isConnected());
  }
}
