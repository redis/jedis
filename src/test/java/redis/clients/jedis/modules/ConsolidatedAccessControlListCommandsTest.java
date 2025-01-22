package redis.clients.jedis.modules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisAccessControlException;

@SinceRedisVersion(value = "8.0.0")
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
  public void aclHashesCommandsTest() {
    // create and enable an user with permission to all keys but no commands
    jedis.aclSetUser(USER_NAME, ">" + USER_PASSWORD, "on", "~*");

    // client object with new user
    try (UnifiedJedis client = new UnifiedJedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().user(USER_NAME).password(USER_PASSWORD).build())) {

      // user can't execute hash commands
      JedisAccessControlException ace = assertThrows("Should throw a NOPERM exception",
          JedisAccessControlException.class,
          () -> client.hgetAll("foo"));
      assertThat(ace.getMessage(), startsWith("NOPERM "));
      assertThat(ace.getMessage(), endsWith(" has no permissions to run the 'hgetall' command"));

      // permit user to hash commands
      jedis.aclSetUser(USER_NAME, "+@hash");

      // user can now execute hash commands
      client.hgetAll("foo");
    }
  }

}
