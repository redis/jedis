package redis.clients.jedis.modules.search;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.resps.Tuple;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class SuggestionTest extends RedisModuleCommandsTestBase {

  private static final String key = "suggest";

  @BeforeAll
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public SuggestionTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void addSuggestionAndGetSuggestion() {
    String suggestion = "ANOTHER_WORD";
    String noMatch = "_WORD MISSED";

    assertTrue(client.ftSugAdd(key, suggestion, 1d) > 0,
        suggestion + " should of inserted at least 1");
    assertTrue(client.ftSugAdd(key, noMatch, 1d) > 0, noMatch + " should of inserted at least 1");

    // test that with a partial part of that string will have the entire word returned
    assertEquals(1, client.ftSugGet(key, suggestion.substring(0, 3), true, 5).size(),
        suggestion + " did not get a match with 3 characters");

    // turn off fuzzy start at second word no hit
    assertEquals(0, client.ftSugGet(key, noMatch.substring(1, 6), false, 5).size(),
        noMatch + " no fuzzy and starting at 1, should not match");

    // my attempt to trigger the fuzzy by 1 character
    assertEquals(1, client.ftSugGet(key, noMatch.substring(1, 6), true, 5).size(),
        noMatch + " fuzzy is on starting at 1 position should match");
  }

  @Test
  public void addSuggestionIncrAndGetSuggestionFuzzy() {
    String suggestion = "TOPIC OF WORDS";

    // test can add a suggestion string
    assertTrue(client.ftSugAddIncr(key, suggestion, 1d) > 0,
        suggestion + " insert should of returned at least 1");

    // test that the partial part of that string will be returned using fuzzy
    assertEquals(suggestion, client.ftSugGet(key, suggestion.substring(0, 3)).get(0));
  }

  @Test
  public void getSuggestionScores() {
    client.ftSugAdd(key, "COUNT_ME TOO", 1);
    client.ftSugAdd(key, "COUNT", 1);
    client.ftSugAdd(key, "COUNT_ANOTHER", 1);

    String noScoreOrPayload = "COUNT NO PAYLOAD OR COUNT";
    assertTrue(client.ftSugAddIncr(key, noScoreOrPayload, 1) > 1,
        "Count single added should return more than 1");

    List<Tuple> result = client.ftSugGetWithScores(key, "COU");
    assertEquals(4, result.size());
    result.forEach(tuple -> assertTrue(tuple.getScore() < .999,
        "Assert that a suggestion has a score not default 1 "));
  }

  @Test
  public void getSuggestionMax() {
    client.ftSugAdd(key, "COUNT_ME TOO", 1);
    client.ftSugAdd(key, "COUNT", 1);
    client.ftSugAdd(key, "COUNTNO PAYLOAD OR COUNT", 1);

    // test that with a partial part of that string will have the entire word returned
    assertEquals( 3, client.ftSugGetWithScores(key, "COU", true, 10).size(),"3 suggestions");
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

    assertTrue(client.ftSugDel(key, "ANOTHER ENTRY"), "Delete suggestion should succeed.");
    assertEquals(1L, client.ftSugLen(key));

    assertFalse(client.ftSugDel(key, "ANOTHER ENTRY"), "Delete suggestion should succeed.");
    assertEquals(1L, client.ftSugLen(key));

    assertFalse(client.ftSugDel(key, "ANOTHER ENTRY THAT IS NOT PRESENT"),
        "Delete suggestion should succeed.");
    assertEquals(1L, client.ftSugLen(key));

    client.ftSugAdd(key, "LAST ENTRY", 1);
    assertEquals(2L, client.ftSugLen(key));
  }

}
