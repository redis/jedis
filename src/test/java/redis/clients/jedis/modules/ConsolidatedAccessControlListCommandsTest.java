package redis.clients.jedis.modules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.redis.test.annotations.SinceRedisVersion;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.bloom.RedisBloomProtocol.*;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.json.JsonProtocol.JsonCommand;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.SafeEncoder;

@SinceRedisVersion(value = "7.9.0")
@RunWith(Parameterized.class)
public class ConsolidatedAccessControlListCommandsTest extends JedisCommandsTestBase {

  public static final String USER_NAME = "moduser";
  public static final String USER_PASSWORD = "secret";

  public ConsolidatedAccessControlListCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    try {
      jedis.aclDelUser(USER_NAME);
    } catch (Exception e) { }
    super.tearDown();
  }

  @Test
  public void listACLCategoriesTest() {
    assertThat(jedis.aclCat(),
        Matchers.hasItems("bloom", "cuckoo", "cms", "topk", "tdigest",
            "search", "timeseries", "json"));
  }

  @Test
  public void grantBloomCommandCatTest() {
    grantModuleCommandCatTest("bloom", BloomFilterCommand.RESERVE, client -> client.bfReserve("foo", 0.01, 10_000));
  }

  @Test
  public void grantCuckooCommandCatTest() {
    grantModuleCommandCatTest("cuckoo", CuckooFilterCommand.RESERVE, client -> client.cfReserve("foo", 10_000));
  }

  @Test
  public void grantCmsCommandCatTest() {
    grantModuleCommandCatTest("cms", CountMinSketchCommand.INITBYDIM, client -> client.cmsInitByDim("foo", 16, 4));
  }

  @Test
  public void grantTopkModuleCommandCatTest() {
    grantModuleCommandCatTest("topk", TopKCommand.RESERVE, client -> client.topkReserve("foo", 1000));
  }

  @Test
  public void grantTdigestCommandCatTest() {
    grantModuleCommandCatTest("tdigest", TDigestCommand.CREATE, client -> client.tdigestCreate("foo"));
  }

  @org.junit.Ignore
  @Test
  public void grantSearchCommandCatTest() {
    grantModuleCommandCatTest("search", SearchCommand.CREATE,
        client -> client.ftCreate("index", Collections.emptySet()));
  }

  @Test
  public void grantTimeseriesCommandCatTest() {
    grantModuleCommandCatTest("timeseries", TimeSeriesCommand.CREATE, client -> client.tsCreate("foo"));
  }

  @Test
  public void grantJsonCommandCatTest() {
    grantModuleCommandCatTest("json", JsonCommand.GET, client -> client.jsonGet("foo"));
  }

  private void grantModuleCommandCatTest(String category, ProtocolCommand command, Consumer<UnifiedJedis> operation) {
    // create and enable an user with permission to all keys but no commands
    jedis.aclSetUser(USER_NAME, ">" + USER_PASSWORD, "on", "~*");

    // client object with new user
    try (UnifiedJedis client = new UnifiedJedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().user(USER_NAME).password(USER_PASSWORD).build())) {

      // user can't execute category commands
      JedisAccessControlException noperm = assertThrows("Should throw a NOPERM exception",
          JedisAccessControlException.class, () -> operation.accept(client));
      assertEquals(String.format("NOPERM User %s has no permissions to run the '%s' command",
          USER_NAME, SafeEncoder.encode(command.getRaw()).toLowerCase(Locale.ENGLISH)),
          noperm.getMessage());

      // permit user to category commands
      jedis.aclSetUser(USER_NAME, "+@" + category);

      // user can now execute category commands
      operation.accept(client);
    }
  }
}
