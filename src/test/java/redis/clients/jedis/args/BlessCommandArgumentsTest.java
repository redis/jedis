package redis.clients.jedis.args;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.ClusterCommandObjects;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Locks down the wire format of the {@code BLESS} command family.
 */
public class BlessCommandArgumentsTest {

  private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP3);

  private static final byte[] KEY = SafeEncoder.encode("session:42");

  @Test
  public void noEvictFlagUsesServerToken() {
    assertArrayEquals(SafeEncoder.encode("NO-EVICT"), BlessFlag.NO_EVICT.getRaw());
  }

  @Nested
  class StringArguments {

    @Test
    public void blessSet() {
      assertThat(commandObjects.blessSet("session:42", BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Keyword.SET, RawableFactory.from("session:42"),
          BlessFlag.NO_EVICT));
    }

    @Test
    public void blessClear() {
      assertThat(commandObjects.blessClear("session:42", BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Keyword.CLEAR, RawableFactory.from("session:42"),
          BlessFlag.NO_EVICT));
    }

    @Test
    public void blessGet() {
      assertThat(commandObjects.blessGet("session:42").getArguments(),
        hasArguments(Command.BLESS, Keyword.GET, RawableFactory.from("session:42")));
    }

    @Test
    public void blessScan() {
      assertThat(commandObjects.blessScan("0", BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Command.SCAN, RawableFactory.from("0"), BlessFlag.NO_EVICT));
    }

    @Test
    public void blessScanWithCount() {
      assertThat(commandObjects.blessScan("17", BlessFlag.NO_EVICT, 100).getArguments(),
        hasArguments(Command.BLESS, Command.SCAN, RawableFactory.from("17"), BlessFlag.NO_EVICT,
          Keyword.COUNT, RawableFactory.from(100)));
    }
  }

  @Nested
  class BinaryArguments {

    @Test
    public void blessSet() {
      assertThat(commandObjects.blessSet(KEY, BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Keyword.SET, RawableFactory.from(KEY), BlessFlag.NO_EVICT));
    }

    @Test
    public void blessClear() {
      assertThat(commandObjects.blessClear(KEY, BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Keyword.CLEAR, RawableFactory.from(KEY), BlessFlag.NO_EVICT));
    }

    @Test
    public void blessGet() {
      assertThat(commandObjects.blessGet(KEY).getArguments(),
        hasArguments(Command.BLESS, Keyword.GET, RawableFactory.from(KEY)));
    }

    @Test
    public void blessScan() {
      byte[] cursor = SafeEncoder.encode("0");
      assertThat(commandObjects.blessScan(cursor, BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Command.SCAN, RawableFactory.from(cursor), BlessFlag.NO_EVICT));
    }

    @Test
    public void blessScanWithCount() {
      byte[] cursor = SafeEncoder.encode("17");
      assertThat(commandObjects.blessScan(cursor, BlessFlag.NO_EVICT, 5).getArguments(),
        hasArguments(Command.BLESS, Command.SCAN, RawableFactory.from(cursor), BlessFlag.NO_EVICT,
          Keyword.COUNT, RawableFactory.from(5)));
    }
  }

  @Nested
  class ClusterArguments {

    private final ClusterCommandObjects clusterCommandObjects = new ClusterCommandObjects(
        RedisProtocol.RESP3);

    @Test
    public void keyedCommandsAreNotOverridden() {
      assertThat(clusterCommandObjects.blessSet("session:42", BlessFlag.NO_EVICT).getArguments(),
        hasArguments(Command.BLESS, Keyword.SET, RawableFactory.from("session:42"),
          BlessFlag.NO_EVICT));
    }

    @Test
    public void blessScanIsRejectedBecauseCursorIsNodeLocal() {
      assertThrows(UnsupportedOperationException.class,
        () -> clusterCommandObjects.blessScan("0", BlessFlag.NO_EVICT));
      assertThrows(UnsupportedOperationException.class,
        () -> clusterCommandObjects.blessScan("0", BlessFlag.NO_EVICT, 10));
      assertThrows(UnsupportedOperationException.class,
        () -> clusterCommandObjects.blessScan(SafeEncoder.encode("0"), BlessFlag.NO_EVICT));
      assertThrows(UnsupportedOperationException.class,
        () -> clusterCommandObjects.blessScan(SafeEncoder.encode("0"), BlessFlag.NO_EVICT, 10));
    }
  }
}
