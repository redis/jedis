package redis.clients.jedis.commands.unified.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.search.*;

/**
 * Base test class for Spell Check commands using the UnifiedJedis pattern. Tests FT.SPELLCHECK,
 * FT.DICTADD, FT.DICTDEL, FT.DICTDUMP.
 */
@Tag("search")
public abstract class SpellCheckCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String INDEX = "spellcheckidx";

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public SpellCheckCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected void clearData() {
    // Drop search index if it exists
    try {
      jedis.ftDropIndex(INDEX);
    } catch (Exception e) {
      // Index might not exist, ignore
    }
    // Clear dictionaries
    try {
      jedis.ftDictDel("dict", jedis.ftDictDump("dict").toArray(new String[0]));
    } catch (Exception e) {
      // Dictionary might not exist, ignore
    }
    try {
      jedis.ftDictDel("sport", jedis.ftDictDump("sport").toArray(new String[0]));
    } catch (Exception e) {
      // Dictionary might not exist, ignore
    }
    try {
      jedis.ftDictDel("exclude", jedis.ftDictDump("exclude").toArray(new String[0]));
    } catch (Exception e) {
      // Dictionary might not exist, ignore
    }
    // Then call parent's clearData which does flushAll
    super.clearData();
  }

  protected void addDocument(String key, Map<String, String> map) {
    jedis.hset(key, map);
  }

  protected static Map<String, String> toStringMap(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }

  @Test
  public void spellCheck() {
    Schema sc = new Schema().addTextField("name", 1.0).addTextField("body", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    addDocument("doc1", toStringMap("name", "name1", "body", "body1"));
    addDocument("doc2", toStringMap("name", "name2", "body", "body2"));
    addDocument("doc3", toStringMap("name", "name2", "body", "name2"));

    // "name" is not an exact term in the index, so it returns suggestions from indexed terms
    Map<String, Map<String, Double>> reply = jedis.ftSpellCheck(INDEX, "name");
    assertEquals(Collections.singleton("name"), reply.keySet());
    assertTrue(reply.get("name").containsKey("name1"));
    assertTrue(reply.get("name").containsKey("name2"));

    // "name1" exists exactly in the index, so no suggestions needed
    reply = jedis.ftSpellCheck(INDEX, "name1");
    assertEquals(Collections.emptyMap(), reply);

    // "name3" doesn't exist but is close to "name1" and "name2"
    reply = jedis.ftSpellCheck(INDEX, "name3");
    assertEquals(1, reply.size());
    assertTrue(reply.containsKey("name3"));
    assertTrue(reply.get("name3").containsKey("name1"));
    assertTrue(reply.get("name3").containsKey("name2"));
  }

  @Test
  public void spellCheckWithParams() {
    Schema sc = new Schema().addTextField("text", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    addDocument("doc1", toStringMap("text", "hello world"));
    addDocument("doc2", toStringMap("text", "hello redis"));

    Map<String, Map<String, Double>> result = jedis.ftSpellCheck(INDEX, "helo word",
      FTSpellCheckParams.spellCheckParams().distance(1));

    assertEquals(2, result.size());
    assertTrue(result.containsKey("helo"));
    assertTrue(result.containsKey("word"));
  }

  @Test
  public void dictionary() {
    assertEquals(3L, jedis.ftDictAdd("dict", "foo", "bar", "baz"));
    assertEquals(0L, jedis.ftDictAdd("dict", "foo", "bar", "baz"));
    assertEquals(3L, jedis.ftDictAdd("dict", "alfa", "beta", "gamma"));

    Set<String> dump = jedis.ftDictDump("dict");
    assertEquals(6, dump.size());
    assertTrue(dump.contains("foo"));
    assertTrue(dump.contains("bar"));
    assertTrue(dump.contains("baz"));
    assertTrue(dump.contains("alfa"));
    assertTrue(dump.contains("beta"));
    assertTrue(dump.contains("gamma"));

    assertEquals(3L, jedis.ftDictDel("dict", "alfa", "beta", "gamma"));
    assertEquals(0L, jedis.ftDictDel("dict", "alfa", "beta", "gamma"));

    dump = jedis.ftDictDump("dict");
    assertEquals(3, dump.size());
    assertTrue(dump.contains("foo"));
    assertTrue(dump.contains("bar"));
    assertTrue(dump.contains("baz"));

    assertEquals(3L, jedis.ftDictDel("dict", "foo", "bar", "baz"));
  }

  @Test
  public void dictionarySampleKey() {
    assertEquals(3L, jedis.ftDictAddBySampleKey("demo", "dict", "foo", "bar", "baz"));
    assertEquals(0L, jedis.ftDictAddBySampleKey("demo", "dict", "foo", "bar", "baz"));

    Set<String> dump = jedis.ftDictDumpBySampleKey("demo", "dict");
    assertEquals(3, dump.size());
    assertTrue(dump.contains("foo"));
    assertTrue(dump.contains("bar"));
    assertTrue(dump.contains("baz"));

    assertEquals(3L, jedis.ftDictDelBySampleKey("demo", "dict", "foo", "bar", "baz"));
    assertEquals(0L, jedis.ftDictDelBySampleKey("demo", "dict", "foo", "bar", "baz"));
  }

  @Test
  public void spellCheckIncludeDict() {
    Schema sc = new Schema().addTextField("name", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    addDocument("doc1", toStringMap("name", "football"));

    assertEquals(3L, jedis.ftDictAdd("sport", "sport", "basketball", "hockey"));

    Map<String, Map<String, Double>> result = jedis.ftSpellCheck(INDEX, "sports",
      FTSpellCheckParams.spellCheckParams().includeTerm("sport"));
    assertEquals(1, result.size());
    assertTrue(result.containsKey("sports"));
    assertTrue(result.get("sports").containsKey("sport"));
  }

  @Test
  public void crossTermDictionary() {
    // Based on the original test from SpellCheckTest
    Schema sc = new Schema().addTextField("report", 1.0);
    assertEquals("OK", jedis.ftCreate(INDEX, IndexOptions.defaultOptions(), sc));

    assertEquals(6L, jedis.ftDictAdd("slang", "timmies", "toque", "toonie", "serviette",
      "kerfuffle", "chesterfield"));

    Map<String, Map<String, Double>> result = jedis.ftSpellCheck(INDEX, "Tooni toque kerfuffle",
      FTSpellCheckParams.spellCheckParams().includeTerm("slang").excludeTerm("slang"));
    // "tooni" should have "toonie" as suggestion from the include dict
    // but "toque" and "kerfuffle" are exact matches in the dictionary so no suggestions for them
    assertEquals(Collections.singletonMap("tooni", Collections.singletonMap("toonie", 0d)), result);
  }
}
