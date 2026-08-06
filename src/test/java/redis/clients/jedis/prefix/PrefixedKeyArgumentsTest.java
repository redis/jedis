package redis.clients.jedis.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Every key argument must reach the wire through {@link redis.clients.jedis.CommandArguments#key},
 * which is where the {@link redis.clients.jedis.CommandKeyArgumentPreProcessor} is applied and
 * where the key is recorded for cluster hash slot computation.
 */
public class PrefixedKeyArgumentsTest {

  private static final String PREFIX = "test-prefix:";

  private CommandObjects commandObjects;

  @BeforeEach
  public void setUp() {
    commandObjects = new CommandObjects(RedisProtocol.RESP3);
    commandObjects.setKeyArgumentPreProcessor(new PrefixedKeyArgumentPreProcessor(PREFIX));
  }

  private static List<String> argsOf(CommandObject<?> commandObject) {
    List<String> args = new ArrayList<>();
    for (Rawable arg : commandObject.getArguments()) {
      args.add(SafeEncoder.encode(arg.getRaw()));
    }
    return args;
  }

  private static void assertBothKeysPrefixed(CommandObject<?> commandObject) {
    List<String> args = argsOf(commandObject);
    assertEquals(PREFIX + "dest", args.get(1));
    assertEquals(PREFIX + "src", args.get(2));
    assertEquals(
      new java.util.HashSet<>(Arrays.asList(JedisClusterCRC16.getSlot(PREFIX + "dest"),
        JedisClusterCRC16.getSlot(PREFIX + "src"))),
      commandObject.getArguments().getKeyHashSlots());
  }

  @Test
  public void zrangestorePrefixesSourceKey() {
    assertBothKeysPrefixed(commandObjects.zrangestore("dest", "src", new ZRangeParams(0, -1)));
    assertBothKeysPrefixed(commandObjects.zrangestore(SafeEncoder.encode("dest"),
      SafeEncoder.encode("src"), new ZRangeParams(0, -1)));
  }

  @Test
  public void geosearchStorePrefixesSourceKey() {
    GeoSearchParam params = GeoSearchParam.geoSearchParam().fromLonLat(1d, 2d).byRadius(3d,
      GeoUnit.KM);

    assertBothKeysPrefixed(commandObjects.geosearchStore("dest", "src", params));
    assertBothKeysPrefixed(commandObjects.geosearchStoreStoreDist("dest", "src", params));
    assertBothKeysPrefixed(commandObjects.geosearchStore("dest", "src", "member", 3d, GeoUnit.KM));
    assertBothKeysPrefixed(
      commandObjects.geosearchStore("dest", "src", "member", 3d, 4d, GeoUnit.KM));

    byte[] dest = SafeEncoder.encode("dest");
    byte[] src = SafeEncoder.encode("src");
    assertBothKeysPrefixed(commandObjects.geosearchStore(dest, src, params));
    assertBothKeysPrefixed(commandObjects.geosearchStoreStoreDist(dest, src, params));
    assertBothKeysPrefixed(
      commandObjects.geosearchStore(dest, src, SafeEncoder.encode("member"), 3d, GeoUnit.KM));
    assertBothKeysPrefixed(
      commandObjects.geosearchStore(dest, src, SafeEncoder.encode("member"), 3d, 4d, GeoUnit.KM));
  }

  @Test
  public void evalVarargsPrefixesKeysButNotArguments() {
    // EVAL script numkeys key arg -- only the first numkeys params are keys
    List<String> expected = Arrays.asList("EVAL", "return 1", "1", PREFIX + "key", "arg");

    assertEquals(expected, argsOf(commandObjects.eval("return 1", 1, "key", "arg")));
    assertEquals(expected, argsOf(commandObjects.eval(SafeEncoder.encode("return 1"), 1,
      SafeEncoder.encode("key"), SafeEncoder.encode("arg"))));

    // same command through the List overload, which has always prefixed correctly
    assertEquals(expected,
      argsOf(commandObjects.eval("return 1", Arrays.asList("key"), Arrays.asList("arg"))));
  }

  @Test
  public void evalshaVarargsPrefixesKeysButNotArguments() {
    List<String> expected = Arrays.asList("EVALSHA", "sha1", "1", PREFIX + "key", "arg");

    assertEquals(expected, argsOf(commandObjects.evalsha("sha1", 1, "key", "arg")));
    assertEquals(expected, argsOf(commandObjects.evalsha(SafeEncoder.encode("sha1"), 1,
      SafeEncoder.encode("key"), SafeEncoder.encode("arg"))));

    assertEquals(expected,
      argsOf(commandObjects.evalsha("sha1", Arrays.asList("key"), Arrays.asList("arg"))));
  }
}
