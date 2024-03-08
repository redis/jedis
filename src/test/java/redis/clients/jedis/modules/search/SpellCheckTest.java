package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.search.FTSpellCheckParams;
import redis.clients.jedis.search.schemafields.TextField;

@RunWith(Parameterized.class)
public class SpellCheckTest extends RedisModuleCommandsTestBase {

  private static final String index = "spellcheck";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  public SpellCheckTest(RedisProtocol protocol) {
    super(protocol);
  }

  private static Map<String, String> toMap(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }

  @Test
  public void dictionary() {
    assertEquals(3L, client.ftDictAdd("dict", "foo", "bar", "hello world"));
    assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "hello world")), client.ftDictDump("dict"));
    assertEquals(3L, client.ftDictDel("dict", "foo", "bar", "hello world"));
    assertEquals(Collections.emptySet(), client.ftDictDump("dict"));
  }

  @Test
  public void dictionaryBySampleKey() {
    assertEquals(3L, client.ftDictAddBySampleKey(index, "dict", "foo", "bar", "hello world"));
    assertEquals(new HashSet<>(Arrays.asList("foo", "bar", "hello world")),
        client.ftDictDumpBySampleKey(index, "dict"));
    assertEquals(3L, client.ftDictDelBySampleKey(index, "dict", "foo", "bar", "hello world"));
    assertEquals(Collections.emptySet(), client.ftDictDumpBySampleKey(index, "dict"));
  }

  @Test
  public void basicSpellCheck() {
    client.ftCreate(index, TextField.of("name"), TextField.of("body"));
    client.hset("doc1", toMap("name", "name1", "body", "body1"));
    client.hset("doc2", toMap("name", "name2", "body", "body2"));
    client.hset("doc3", toMap("name", "name2", "body", "name2"));

    Map<String, Map<String, Double>> reply = client.ftSpellCheck(index, "name");
    assertEquals(Collections.singleton("name"), reply.keySet());
    assertEquals(new HashSet<>(Arrays.asList("name1", "name2")), reply.get("name").keySet());
  }

  @Test
  public void crossTermDictionary() {
    client.ftCreate(index, TextField.of("report"));
    client.ftDictAdd("slang", "timmies", "toque", "toonie", "serviette", "kerfuffle", "chesterfield");

    Map<String, Map<String, Double>> expected = Collections.singletonMap("tooni",
        Collections.singletonMap("toonie", 0d));
    assertEquals(expected, client.ftSpellCheck(index, "Tooni toque kerfuffle",
        FTSpellCheckParams.spellCheckParams().includeTerm("slang").excludeTerm("slang")));
  }

  @Test
  public void distanceBound() {
    client.ftCreate(index, TextField.of("name"), TextField.of("body"));
    Assert.assertThrows(JedisDataException.class, () -> client.ftSpellCheck(index, "name",
        FTSpellCheckParams.spellCheckParams().distance(0)));
  }

  @Test
  public void dialectBound() {
    client.ftCreate(index, TextField.of("t"));
    JedisDataException error = Assert.assertThrows(JedisDataException.class,
        () -> client.ftSpellCheck(index, "Tooni toque kerfuffle",
            FTSpellCheckParams.spellCheckParams().dialect(0)));
    MatcherAssert.assertThat(error.getMessage(),
        Matchers.containsString("DIALECT requires a non negative integer"));
  }
}
