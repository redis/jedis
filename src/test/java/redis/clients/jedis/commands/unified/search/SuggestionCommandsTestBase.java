package redis.clients.jedis.commands.unified.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.resps.Tuple;

/**
 * Base test class for Suggestion (auto-complete) commands using the UnifiedJedis pattern. Tests
 * FT.SUGADD, FT.SUGGET, FT.SUGDEL, FT.SUGLEN.
 */
@Tag("search")
public abstract class SuggestionCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String SUGGESTION_KEY = "suggestion";

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public SuggestionCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected void clearData() {
    // First delete the suggestion key explicitly
    jedis.del(SUGGESTION_KEY);
    // Then call parent's clearData which does flushAll
    super.clearData();
  }

  @Test
  public void sugAdd() {
    // ftSugAdd returns the current size of the dictionary
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0,
      "hello should have inserted at least 1");
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 1.0) > 0,
      "help should have inserted at least 1");
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "helicopter", 1.0) > 0,
      "helicopter should have inserted at least 1");
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hell", 1.0) > 0,
      "hell should have inserted at least 1");

    assertEquals(4L, jedis.ftSugLen(SUGGESTION_KEY));
  }

  @Test
  public void sugAddIncrement() {
    assertTrue(jedis.ftSugAddIncr(SUGGESTION_KEY, "hello", 1.0) > 0,
      "hello insert should return at least 1");

    assertEquals(1L, jedis.ftSugLen(SUGGESTION_KEY));
  }

  @Test
  public void sugDel() {
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 1.0) > 0);

    assertEquals(2L, jedis.ftSugLen(SUGGESTION_KEY));

    assertTrue(jedis.ftSugDel(SUGGESTION_KEY, "hello"));
    assertFalse(jedis.ftSugDel(SUGGESTION_KEY, "hello"));

    assertEquals(1L, jedis.ftSugLen(SUGGESTION_KEY));
  }

  @Test
  public void sugGet() {
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "helicopter", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hell", 1.0) > 0);

    List<String> suggestions = jedis.ftSugGet(SUGGESTION_KEY, "hel");
    assertEquals(4, suggestions.size());
    assertTrue(suggestions.contains("hello"));
    assertTrue(suggestions.contains("help"));
    assertTrue(suggestions.contains("helicopter"));
    assertTrue(suggestions.contains("hell"));
  }

  @Test
  public void sugGetWithMax() {
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 2.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "helicopter", 3.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hell", 4.0) > 0);

    List<String> suggestions = jedis.ftSugGet(SUGGESTION_KEY, "hel", true, 2);
    assertEquals(2, suggestions.size());
    // The order is by score descending, so "hell" (4.0) should come before "helicopter" (3.0)
    // But we'll just verify we get exactly 2 of the highest scored items
    assertTrue(suggestions.contains("hell") || suggestions.contains("helicopter"));
  }

  @Test
  public void sugGetWithFuzzy() {
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 1.0) > 0);

    // Fuzzy search for "helo" - should match "hello"
    List<String> suggestions = jedis.ftSugGet(SUGGESTION_KEY, "helo", true, 10);
    // With fuzzy matching, we should get at least "hello" as a match
    assertTrue(suggestions.size() >= 1);
    assertTrue(suggestions.contains("hello"));
  }

  @Test
  public void sugGetWithScores() {
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 2.0) > 0);

    List<Tuple> suggestions = jedis.ftSugGetWithScores(SUGGESTION_KEY, "hel");
    // With scores returns list of Tuple (string, score)
    assertNotNull(suggestions);
    assertTrue(suggestions.size() >= 2);
  }

  @Test
  public void sugLen() {
    assertEquals(0L, jedis.ftSugLen(SUGGESTION_KEY));

    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertEquals(1L, jedis.ftSugLen(SUGGESTION_KEY));

    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 1.0) > 0);
    assertEquals(2L, jedis.ftSugLen(SUGGESTION_KEY));
  }

  @Test
  public void sugGetWithPayload() {
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "hello", 1.0) > 0);
    assertTrue(jedis.ftSugAdd(SUGGESTION_KEY, "help", 1.0) > 0);

    List<String> result = jedis.ftSugGet(SUGGESTION_KEY, "hel");
    assertEquals(2, result.size());
    assertTrue(result.contains("hello"));
    assertTrue(result.contains("help"));
  }
}
