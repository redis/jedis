package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ScanResult;

/**
 * Tests related to <a href="https://redis.io/commands/?group=generic">BLESS</a> commands.
 */
@EnabledOnCommand("BLESS")
public class CommandObjectsBlessCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsBlessCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testBlessSetAndClear() {
    String key = "co-bless-key";
    exec(commandObjects.set(key, "v"));

    Long set = exec(commandObjects.blessSet(key, BlessFlag.NO_EVICT));
    assertThat(set, equalTo(1L));

    List<String> flags = exec(commandObjects.blessGet(key));
    assertThat(flags, hasSize(1));

    Long cleared = exec(commandObjects.blessClear(key, BlessFlag.NO_EVICT));
    assertThat(cleared, equalTo(1L));

    List<String> afterClear = exec(commandObjects.blessGet(key));
    assertThat(afterClear, empty());
  }

  @Test
  public void testBlessSetAndClearBinary() {
    byte[] key = "co-bless-key-b".getBytes();
    exec(commandObjects.set(key, "v".getBytes()));

    Long set = exec(commandObjects.blessSet(key, BlessFlag.NO_EVICT));
    assertThat(set, equalTo(1L));

    List<byte[]> flags = exec(commandObjects.blessGet(key));
    assertThat(flags, hasSize(1));

    Long cleared = exec(commandObjects.blessClear(key, BlessFlag.NO_EVICT));
    assertThat(cleared, equalTo(1L));
  }

  @Test
  public void testBlessGetMissingKeyThrows() {
    assertThrows(JedisDataException.class,
      () -> exec(commandObjects.blessGet("co-bless-missing-key")));
  }

  @Test
  public void testBlessScan() {
    String key = "co-bless-scan-key";
    exec(commandObjects.set(key, "v"));
    exec(commandObjects.blessSet(key, BlessFlag.NO_EVICT));

    ScanResult<String> result = exec(commandObjects.blessScan("0", BlessFlag.NO_EVICT, 1000));
    assertThat(result, org.hamcrest.Matchers.notNullValue());
  }
}
