package redis.clients.jedis.modules.search;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.resps.Tuple;

public class SuggestionTest extends RedisModuleCommandsTestBase {

  private static final String key = "suggest";

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  @Test
  public void addSuggestionAndGetSuggestion() {
    String suggestion = "ANOTHER_WORD";
    String noMatch = "_WORD MISSED";

    assertTrue(suggestion + " should of inserted at least 1", client.ftSugAdd(key, suggestion, 1d) > 0);
    assertTrue(noMatch + " should of inserted at least 1", client.ftSugAdd(key, noMatch, 1d) > 0);

    // test that with a partial part of that string will have the entire word returned
    assertEquals(suggestion + " did not get a match with 3 characters",
        1, client.ftSugGet(key, suggestion.substring(0, 3), true, 5).size());

    // turn off fuzzy start at second word no hit
    assertEquals(noMatch + " no fuzzy and starting at 1, should not match",
        0, client.ftSugGet(key, noMatch.substring(1, 6), false, 5).size());

    // my attempt to trigger the fuzzy by 1 character
    assertEquals(noMatch + " fuzzy is on starting at 1 position should match",
        1, client.ftSugGet(key, noMatch.substring(1, 6), true, 5).size());
  }

  @Test
  public void addSuggestionIncrAndGetSuggestionFuzzy() {
    String suggestion = "TOPIC OF WORDS";

    // test can add a suggestion string
    assertTrue(suggestion + " insert should of returned at least 1", client.ftSugAddIncr(key, suggestion, 1d) > 0);

    // test that the partial part of that string will be returned using fuzzy
    assertEquals(suggestion, client.ftSugGet(key, suggestion.substring(0, 3)).get(0));
  }

  @Test
  public void getSuggestionScores() {
    client.ftSugAdd(key, "COUNT_ME TOO", 1);
    client.ftSugAdd(key, "COUNT", 1);
    client.ftSugAdd(key, "COUNT_ANOTHER", 1);

    String noScoreOrPayload = "COUNT NO PAYLOAD OR COUNT";
    assertTrue("Count single added should return more than 1", client.ftSugAddIncr(key, noScoreOrPayload, 1) > 1);

    List<Tuple> result = client.ftSugGetWithScores(key, "COU");
    assertEquals(4, result.size());
    result.forEach(tuple -> assertTrue("Assert that a suggestion has a score not default 1 ", tuple.getScore() < .999));
  }

  @Test
  public void getSuggestionMax() {
    client.ftSugAdd(key, "COUNT_ME TOO", 1);
    client.ftSugAdd(key, "COUNT", 1);
    client.ftSugAdd(key, "COUNTNO PAYLOAD OR COUNT", 1);

    // test that with a partial part of that string will have the entire word returned
    assertEquals("3 suggestions", 3, client.ftSugGetWithScores(key, "COU", true, 10).size());
    assertEquals(2, client.ftSugGetWithScores(key, "COU", true, 2).size());
  }

  @Test
  public void getSuggestionNoHit() {
    client.ftSugAdd(key, "NO WORD", 0.4);

    assertEquals(emptyList(), client.ftSugGetWithScores(key, "DIF"));
    assertEquals(emptyList(), client.ftSugGet(key, "DIF"));
  }

  @Test
  public void getSuggestionLengthAndDeleteSuggestion() {
    client.ftSugAddIncr(key, "TOPIC OF WORDS", 1);
    client.ftSugAddIncr(key, "ANOTHER ENTRY", 1);
    assertEquals(2L, client.ftSugLen(key));

    assertTrue("Delete suggestion should succeed.", client.ftSugDel(key, "ANOTHER ENTRY"));
    assertEquals(1L, client.ftSugLen(key));

    assertFalse("Delete suggestion should succeed.", client.ftSugDel(key, "ANOTHER ENTRY"));
    assertEquals(1L, client.ftSugLen(key));

    assertFalse("Delete suggestion should succeed.", client.ftSugDel(key, "ANOTHER ENTRY THAT IS NOT PRESENT"));
    assertEquals(1L, client.ftSugLen(key));

    client.ftSugAdd(key, "LAST ENTRY", 1);
    assertEquals(2L, client.ftSugLen(key));
  }

}
