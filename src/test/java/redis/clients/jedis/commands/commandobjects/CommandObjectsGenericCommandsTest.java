package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;

/**
 * Tests related to <a href="https://redis.io/commands/?group=generic">Generic</a> commands.
 */
public class CommandObjectsGenericCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsGenericCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testCopy() {
    String srcKey = "sourceKey";
    String dstKey = "destinationKey";
    int dstDB = 1;

    exec(commandObjects.set(srcKey, "initialValue"));

    Boolean existsAfterSet = exec(commandObjects.exists(srcKey));
    assertThat(existsAfterSet, equalTo(true));

    Boolean copy = exec(commandObjects.copy(srcKey, dstKey, dstDB, true));
    assertThat(copy, equalTo(true));

    assertKeyExists(dstDB, dstKey, "initialValue");

    // Update source
    exec(commandObjects.set(srcKey, "newValue"));

    // Copy again without replace, it fails since dstKey already exists
    Boolean secondCopy = exec(commandObjects.copy(srcKey, dstKey, dstDB, false));
    assertThat(secondCopy, equalTo(false));

    assertKeyExists(dstDB, dstKey, "initialValue");
  }

  @Test
  public void testCopyBinary() {
    String srcKey = "sourceKey";
    String dstKey = "destinationKey";
    int dstDB = 1;

    exec(commandObjects.set(srcKey, "initialValue"));

    Boolean existsAfterSet = exec(commandObjects.exists(srcKey));
    assertThat(existsAfterSet, equalTo(true));

    Boolean copy = exec(commandObjects.copy(
        srcKey.getBytes(), dstKey.getBytes(), dstDB, true));
    assertThat(copy, equalTo(true));

    assertKeyExists(dstDB, dstKey, "initialValue");

    // Update source
    exec(commandObjects.set(srcKey, "newValue"));

    // Copy again without replace, it will fail
    Boolean secondCopy = exec(commandObjects.copy(srcKey.getBytes(), dstKey.getBytes(), dstDB, false));
    assertThat(secondCopy, equalTo(false));

    assertKeyExists(dstDB, dstKey, "initialValue");
  }

  private void assertKeyExists(int dstDb, String key, Object expectedValue) {
    // Cheat and use Jedis, it gives us access to any db.
    try (Jedis jedis = new Jedis(nodeInfo)) {
      jedis.auth("foobared");
      jedis.select(dstDb);
      assertThat(jedis.get(key), equalTo(expectedValue));
    }
  }

}
