package redis.clients.jedis.search.aggr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;

/**
 * Unit tests for {@link CollectReducer} — verify that the builder produces the exact token layout
 * that the Redis Search COLLECT grammar expects.
 */
public class CollectReducerTest {

  // -- defaults / metadata -------------------------------------------------

  @Test
  public void newReducerHasCollectNameAndNoField() {
    CollectReducer collect = Reducers.collect();
    assertEquals("COLLECT", collect.getName());
    assertNull(collect.getField(), "COLLECT does not use the parent's single-field slot");
    assertNull(collect.getAlias());
  }

  @Test
  public void aliasIsRecordedOnTheReducer() {
    CollectReducer collect = Reducers.collect().fields("@fruit");
    Reducer same = collect.as("items");
    assertSame(collect, same, "as() returns the same instance for chaining");
    assertEquals("items", collect.getAlias());
  }

  // -- FIELDS form ---------------------------------------------------------

  @Test
  public void fieldsExplicitProducesNumFieldsAndNames() {
    // REDUCE COLLECT 4 FIELDS 2 @fruit @sweetness AS items
    CollectReducer collect = Reducers.collect().fields("@fruit", "@sweetness");
    collect.as("items");

    List<Object> args = serialize(collect);
    assertThat(args, contains(SearchKeyword.REDUCE, "COLLECT", 4, SearchKeyword.FIELDS, 2, "@fruit",
      "@sweetness", SearchKeyword.AS, "items"));
  }

  @Test
  public void fieldsAllProducesAsteriskWithoutCount() {
    // REDUCE COLLECT 2 FIELDS * AS top
    CollectReducer collect = Reducers.collect().fieldsAll();
    collect.as("top");

    List<Object> args = serialize(collect);
    assertThat(args, contains(SearchKeyword.REDUCE, "COLLECT", 2, SearchKeyword.FIELDS,
      Protocol.BYTES_ASTERISK, SearchKeyword.AS, "top"));
  }

  @Test
  public void fieldsCanBeAppendedAcrossCalls() {
    CollectReducer collect = Reducers.collect().fields("@a", "@b").fields("@c");
    collect.as("items");

    List<Object> args = serialize(collect);
    // narg = FIELDS 3 @a @b @c = 5 tokens
    assertEquals(5, args.get(2));
    assertThat(args, contains(SearchKeyword.REDUCE, "COLLECT", 5, SearchKeyword.FIELDS, 3, "@a",
      "@b", "@c", SearchKeyword.AS, "items"));
  }

  @Test
  public void fieldsAllAfterExplicitFieldsThrows() {
    assertThrows(IllegalStateException.class,
      () -> Reducers.collect().fields("@fruit").fieldsAll());
  }

  @Test
  public void explicitFieldsAfterFieldsAllThrows() {
    assertThrows(IllegalStateException.class,
      () -> Reducers.collect().fieldsAll().fields("@fruit"));
  }

  @Test
  public void missingFieldsConfigurationThrowsAtSerialization() {
    CollectReducer collect = Reducers.collect();
    collect.as("items");
    assertThrows(IllegalStateException.class, () -> collect.addArgs(new ArrayList<>()));
  }

  // -- SORTBY --------------------------------------------------------------

  @Test
  public void sortByDescOnly() {
    // narg = FIELDS 1 @fruit SORTBY 2 @sweetness DESC = 7
    CollectReducer collect = Reducers.collect().fields("@fruit").sortByDesc("@sweetness");
    collect.as("top");

    List<Object> args = serialize(collect);
    assertThat(args, contains(SearchKeyword.REDUCE, "COLLECT", 7, SearchKeyword.FIELDS, 1, "@fruit",
      SearchKeyword.SORTBY, 2, "@sweetness", "DESC", SearchKeyword.AS, "top"));
  }

  @Test
  public void sortByAscShortcut() {
    CollectReducer collect = Reducers.collect().fields("@fruit").sortByAsc("@sweetness");
    collect.as("top");

    List<Object> args = serialize(collect);
    int sortByIdx = args.indexOf(SearchKeyword.SORTBY);
    assertTrue(sortByIdx > 0, "SORTBY token must appear in the serialized args");
    assertEquals(2, args.get(sortByIdx + 1));
    assertEquals("@sweetness", args.get(sortByIdx + 2));
    assertEquals("ASC", args.get(sortByIdx + 3));
  }

  @Test
  public void sortByMultipleFieldsAccumulates() {
    // narg = FIELDS 1 @x SORTBY 4 @a DESC @b ASC = 9
    CollectReducer collect = Reducers.collect().fields("@x").sortBy(SortedField.desc("@a"),
      SortedField.asc("@b"));
    collect.as("top");

    List<Object> args = serialize(collect);
    assertThat(args, contains(SearchKeyword.REDUCE, "COLLECT", 9, SearchKeyword.FIELDS, 1, "@x",
      SearchKeyword.SORTBY, 4, "@a", "DESC", "@b", "ASC", SearchKeyword.AS, "top"));
  }

  @Test
  public void successiveSortByCallsAppend() {
    CollectReducer collect = Reducers.collect().fields("@x").sortBy(SortedField.desc("@a"))
        .sortBy(SortedField.asc("@b"));
    collect.as("top");

    List<Object> args = serialize(collect);
    int sortByIdx = args.indexOf(SearchKeyword.SORTBY);
    // 2 sort fields × 2 tokens each = 4
    assertEquals(4, args.get(sortByIdx + 1));
  }

  // -- LIMIT ---------------------------------------------------------------

  @Test
  public void limitCountOnlyDefaultsOffsetZero() {
    // narg = FIELDS 1 @x LIMIT 0 5 = 6
    CollectReducer collect = Reducers.collect().fields("@x").limit(5);
    collect.as("top");

    List<Object> args = serialize(collect);
    assertThat(args, contains(SearchKeyword.REDUCE, "COLLECT", 6, SearchKeyword.FIELDS, 1, "@x",
      SearchKeyword.LIMIT, 0, 5, SearchKeyword.AS, "top"));
  }

  @Test
  public void limitOffsetCount() {
    CollectReducer collect = Reducers.collect().fields("@x").limit(3, 7);
    collect.as("top");

    List<Object> args = serialize(collect);
    int limitIdx = args.indexOf(SearchKeyword.LIMIT);
    assertEquals(3, args.get(limitIdx + 1));
    assertEquals(7, args.get(limitIdx + 2));
  }

  @Test
  public void limitRejectsNegativeOffset() {
    assertThrows(IllegalArgumentException.class, () -> Reducers.collect().limit(-1, 5));
  }

  @Test
  public void limitRejectsNegativeCount() {
    assertThrows(IllegalArgumentException.class, () -> Reducers.collect().limit(0, -1));
  }

  // -- Full clause ---------------------------------------------------------

  @Test
  public void fullClauseMatchesGrammarOrdering() {
    // FIELDS comes first, then SORTBY, then LIMIT, then AS — even if methods were called
    // in a different order on the builder.
    CollectReducer collect = Reducers.collect().limit(0, 2).sortByDesc("@sweetness")
        .fields("@__key", "@fruit", "@sweetness").sortBy(SortedField.asc("@__key"));
    collect.as("top");

    List<Object> args = serialize(collect);
    // narg = 14: FIELDS 3 @__key @fruit @sweetness SORTBY 4 @sweetness DESC @__key ASC LIMIT 0 2
    assertThat(args,
      contains(SearchKeyword.REDUCE, "COLLECT", 14, SearchKeyword.FIELDS, 3, "@__key", "@fruit",
        "@sweetness", SearchKeyword.SORTBY, 4, "@sweetness", "DESC", "@__key", "ASC",
        SearchKeyword.LIMIT, 0, 2, SearchKeyword.AS, "top"));
  }

  @Test
  public void nargMatchesNumberOfTokensBetweenCollectAndAs() {
    CollectReducer collect = Reducers.collect().fieldsAll().sortBy(SortedField.desc("@s")).limit(0,
      2);
    collect.as("top");

    List<Object> args = serialize(collect);
    int collectIdx = args.indexOf("COLLECT");
    int asIdx = args.indexOf(SearchKeyword.AS);
    int declaredNarg = (Integer) args.get(collectIdx + 1);

    assertEquals(asIdx - collectIdx - 2, declaredNarg,
      "narg must equal the number of tokens between COLLECT <narg> and AS");
  }

  @Test
  public void aliasIsOptional() {
    CollectReducer collect = Reducers.collect().fields("@fruit");
    List<Object> args = serialize(collect);
    // No AS / alias appended when not set.
    assertThat(args.contains(SearchKeyword.AS), is(false));
  }

  // -- helpers -------------------------------------------------------------

  private static List<Object> serialize(CollectReducer collect) {
    List<Object> args = new ArrayList<>();
    collect.addArgs(args);
    return args;
  }
}
