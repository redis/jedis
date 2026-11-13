package redis.clients.jedis.params;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static redis.clients.jedis.params.ZMoveMemberParams.zMoveMemberParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.SafeEncoder;

public class ZMoveMemberParamsTest {

  private static List<String> toStrings(CommandArguments args) {
    List<String> out = new ArrayList<>();
    for (Rawable r : args) {
      out.add(SafeEncoder.encode(r.getRaw()));
    }
    return out;
  }

  /** The tokens the params add after the command name. */
  private static List<String> tokens(ZMoveMemberParams params) {
    CommandArguments args = new CommandArguments(Protocol.Command.ZMOVEMEMBER);
    params.addParams(args);
    List<String> all = toStrings(args);
    return all.subList(1, all.size());
  }

  /** Scenario ZMOV-A16: params with no condition add no arguments. */
  @Test
  public void noConditionAddsNothing() {
    assertEquals(Collections.emptyList(), tokens(zMoveMemberParams()));
    assertEquals(Collections.emptyList(), tokens(new ZMoveMemberParams()));
  }

  /** Scenario ZMOV-A17: {@code nx()} adds NX; {@code xx()} adds XX. */
  @Test
  public void nxAndXx() {
    assertEquals(Collections.singletonList("NX"), tokens(zMoveMemberParams().nx()));
    assertEquals(Collections.singletonList("XX"), tokens(zMoveMemberParams().xx()));
  }

  /** Scenario ZMOV-A18: the last condition wins; NX and XX are never both sent. */
  @Test
  public void lastConditionWins() {
    assertEquals(Collections.singletonList("XX"), tokens(zMoveMemberParams().nx().xx()));
    assertEquals(Collections.singletonList("NX"), tokens(zMoveMemberParams().xx().nx()));
  }

  /** Scenario ZMOV-A19: equals and hashCode hold over the condition. */
  @Test
  public void equalsAndHashCode() {
    assertEquals(zMoveMemberParams().nx(), zMoveMemberParams().nx());
    assertEquals(zMoveMemberParams().nx().hashCode(), zMoveMemberParams().nx().hashCode());
    assertEquals(zMoveMemberParams(), new ZMoveMemberParams());
    assertEquals(zMoveMemberParams().hashCode(), new ZMoveMemberParams().hashCode());
    assertEquals(zMoveMemberParams().xx().nx(), zMoveMemberParams().nx());

    assertNotEquals(zMoveMemberParams().nx(), zMoveMemberParams().xx());
    assertNotEquals(zMoveMemberParams().nx(), zMoveMemberParams());
  }

  /** Scenario ZMOV-A20: empty params build the same command as no params. */
  @Test
  public void emptyParamsSameAsNoParams() {
    CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP2);
    ZMoveMemberParams empty = zMoveMemberParams();
    CommandObject<List<Tuple>> withParams = commandObjects.zmovemember("{z}s", "{z}d", empty, "a");
    CommandObject<List<Tuple>> without = commandObjects.zmovemember("{z}s", "{z}d", "a");
    assertEquals(toStrings(without.getArguments()), toStrings(withParams.getArguments()));
  }
}
