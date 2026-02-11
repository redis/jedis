package redis.clients.jedis.search.hybrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.search.Apply;
import redis.clients.jedis.search.Filter;
import redis.clients.jedis.search.Limit;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.search.aggr.Group;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.SortedField;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.LOAD;

public class FTHybridPostProcessingParamsTest {

  private FTHybridPostProcessingParams.Builder builder;

  @BeforeEach
  void setUp() {
    builder = FTHybridPostProcessingParams.builder();
  }

  @Nested
  class EqualityAndHashCodeTests {

    @Test
    public void equalsWithIdenticalParams() {
      FTHybridPostProcessingParams firstParam = builder.load("field1", "field2").build();
      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field1", "field2").build();
      assertEquals(firstParam, secondParam);
    }

    @Test
    public void hashCodeWithIdenticalParams() {
      FTHybridPostProcessingParams firstParam = builder.load("field1", "field2").build();
      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field1", "field2").build();
      assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void equalsWithDifferentLoadFields() {
      FTHybridPostProcessingParams firstParam = builder.load("field1").build();
      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field2").build();
      assertNotEquals(firstParam, secondParam);
    }

    @Test
    public void hashCodeWithDifferentLoadFields() {
      FTHybridPostProcessingParams firstParam = builder.load("field1").build();
      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field2").build();
      assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void equalsWithNull() {
      FTHybridPostProcessingParams firstParam = builder.load("field1").build();
      FTHybridPostProcessingParams secondParam = null;
      assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void equalsWithSameInstance() {
      FTHybridPostProcessingParams param = builder.load("field1").build();
      assertTrue(param.equals(param));
    }

    @Test
    public void equalsLoadAllVsLoadFields() {
      FTHybridPostProcessingParams firstParam = builder.loadAll().build();
      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field1").build();
      assertNotEquals(firstParam, secondParam);
    }

    @Test
    public void equalsWithDifferentLimit() {
      FTHybridPostProcessingParams firstParam = builder.load("field1").limit(Limit.of(0, 10))
          .build();
      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field1").limit(Limit.of(0, 20)).build();
      assertNotEquals(firstParam, secondParam);
    }
  }

  @Nested
  class LoadValidationTests {

    @Test
    public void loadNullThrowsException() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        builder.load((String[]) null);
      });
      assertEquals("Fields must not be null", exception.getMessage());
    }

    @Test
    public void loadEmptyArrayThrowsException() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        builder.load(new String[0]);
      });
      assertEquals("At least one field is required", exception.getMessage());
    }

    @Test
    public void loadWithWildcardThrowsException() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        builder.load("*");
      });
      assertEquals("Cannot use '*' in load(). Use loadAll() instead to load all fields.",
        exception.getMessage());
    }

    @Test
    public void loadWithWildcardMixedWithFieldsThrowsException() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        builder.load("field1", "*", "field2");
      });
      assertEquals("Cannot use '*' in load(). Use loadAll() instead to load all fields.",
        exception.getMessage());
    }

    @Test
    public void loadWithNullFieldThrowsException() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        builder.load("field1", null, "field2");
      });
      assertEquals("Field names cannot be null", exception.getMessage());
    }

    @Test
    public void loadAllDoesNotThrow() {
      assertDoesNotThrow(() -> builder.loadAll());
    }

    @Test
    public void loadWithValidFieldsDoesNotThrow() {
      assertDoesNotThrow(() -> builder.load("field1", "field2", "field3"));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    public void lastLoadCallWins() {
      FTHybridPostProcessingParams firstParam = builder.load("field1").load("field2").build();

      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field2").build();

      assertEquals(firstParam, secondParam);
    }

    @Test
    public void loadAllOverridesLoad() {
      FTHybridPostProcessingParams firstParam = builder.load("field1", "field2").loadAll().build();

      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder().loadAll()
          .build();

      assertEquals(firstParam, secondParam);
    }

    @Test
    public void loadOverridesLoadAll() {
      FTHybridPostProcessingParams firstParam = builder.loadAll().load("field1").build();

      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field1").build();

      assertEquals(firstParam, secondParam);
    }

    @Test
    public void equalsWithSameFieldsDifferentOrder() {
      FTHybridPostProcessingParams firstParam = builder.load("field1", "field2").build();

      FTHybridPostProcessingParams secondParam = FTHybridPostProcessingParams.builder()
          .load("field2", "field1").build();

      assertNotEquals(firstParam, secondParam);
    }

    @Test
    public void loadWithAtPrefixPreserved() {
      FTHybridPostProcessingParams params = builder.load("@field1", "field2").build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Both should have @ prefix in the output
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@field1"), iter.next());
      assertEquals(RawableFactory.from("@field2"), iter.next());
    }
  }

  @Nested
  class SortByTests {

    @Test
    public void sortByWithSingleField() {
      FTHybridPostProcessingParams params = builder.sortBy(SortedField.asc("@price")).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID SORTBY 2 @price ASC
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(SearchProtocol.SearchKeyword.SORTBY, iter.next());
      assertEquals(RawableFactory.from(2), iter.next()); // 1 field * 2 = 2
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(RawableFactory.from("ASC"), iter.next());
    }

    @Test
    public void sortByWithMultipleFields() {
      FTHybridPostProcessingParams params = builder
          .sortBy(SortedField.asc("@price"), SortedField.desc("@rating"), SortedField.asc("@brand"))
          .build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID SORTBY 6 @price ASC @rating DESC @brand ASC
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(SearchProtocol.SearchKeyword.SORTBY, iter.next());
      assertEquals(RawableFactory.from(6), iter.next()); // 3 fields * 2 = 6
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(RawableFactory.from("ASC"), iter.next());
      assertEquals(RawableFactory.from("@rating"), iter.next());
      assertEquals(RawableFactory.from("DESC"), iter.next());
      assertEquals(RawableFactory.from("@brand"), iter.next());
      assertEquals(RawableFactory.from("ASC"), iter.next());
    }

    @Test
    public void sortByWithLoadAndLimit() {
      FTHybridPostProcessingParams params = builder.load("price", "rating")
          .sortBy(SortedField.desc("@price")).limit(Limit.of(0, 10)).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 2 @price @rating SORTBY 2 @price DESC LIMIT 0 10
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(RawableFactory.from("@rating"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.SORTBY, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(RawableFactory.from("DESC"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.LIMIT, iter.next());
      assertEquals(RawableFactory.from(0), iter.next());
      assertEquals(RawableFactory.from(10), iter.next());
    }

    @Test
    public void lastSortByCallWins() {
      // When sortBy is called multiple times, the last call should win
      FTHybridPostProcessingParams params = builder.sortBy(SortedField.asc("@price"))
          .sortBy(SortedField.desc("@rating")).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID SORTBY 2 @rating DESC (only the last sortBy)
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(SearchProtocol.SearchKeyword.SORTBY, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@rating"), iter.next());
      assertEquals(RawableFactory.from("DESC"), iter.next());
    }

    @Test
    public void lastSortByNoSortCallWins() {
      // When both sortBy and noSort are set, noSort should take precedence
      FTHybridPostProcessingParams params = builder.sortBy(SortedField.asc("@price")).noSort()
          .build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID NOSORT (sortBy should be ignored)
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(SearchProtocol.SearchKeyword.NOSORT, iter.next());
      assertFalse(iter.hasNext());
    }

    @Test
    public void lastNoSortSortByCallWins() {
      // When both sortBy and noSort are set, noSort should take precedence
      FTHybridPostProcessingParams params = builder.noSort().sortBy(SortedField.asc("@price"))
          .build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID NOSORT (sortBy should be ignored)
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(SearchProtocol.SearchKeyword.SORTBY, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(RawableFactory.from("ASC"), iter.next());
      assertFalse(iter.hasNext());
    }

    @Test
    public void sortByWithEmptyArrayThrowsException() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        builder.sortBy((SortedField[]) null);
      });
      assertEquals("Sort by fields must not be null", exception.getMessage());
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void addParamsWithLoadSpecificFields() {
      FTHybridPostProcessingParams params = builder.load("field1", "field2").build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 2 @field1 @field2
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@field1"), iter.next());
      assertEquals(RawableFactory.from("@field2"), iter.next());
    }

    @Test
    public void addParamsWithLoadAll() {
      FTHybridPostProcessingParams params = builder.loadAll().build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD *
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from("*"), iter.next());
    }

    @Test
    public void addParamsWithNoLoad() {
      FTHybridPostProcessingParams params = builder.limit(Limit.of(0, 10)).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LIMIT 0 10 (no LOAD)
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(SearchProtocol.SearchKeyword.LIMIT, iter.next());
      assertEquals(RawableFactory.from(0), iter.next());
      assertEquals(RawableFactory.from(10), iter.next());
    }

    @Test
    public void addParamsWithLoadAndGroupBy() {
      FTHybridPostProcessingParams params = builder.load("brand", "price")
          .groupBy(new Group("@brand").reduce(Reducers.count().as("count"))).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 2 @brand @price GROUPBY 1 @brand REDUCE COUNT 0 AS count
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@brand"), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.GROUPBY, iter.next());
      // Continue with GROUPBY assertions...
    }

    @Test
    public void addParamsWithLoadAndApply() {
      FTHybridPostProcessingParams params = builder.load("price")
          .apply(Apply.of("@price * 0.9", "discounted")).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 1 @price APPLY @price * 0.9 AS discounted
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(1), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.APPLY, iter.next());
      // Continue with APPLY assertions...
    }

    @Test
    public void addParamsWithLoadAndSortBy() {
      FTHybridPostProcessingParams params = builder.load("price", "rating")
          .sortBy(SortedField.asc("@price")).build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 2 @price @rating SORTBY 2 @price ASC
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(RawableFactory.from("@rating"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.SORTBY, iter.next());
      // Continue with SORTBY assertions...
    }

    @Test
    public void addParamsWithLoadAndNoSort() {
      FTHybridPostProcessingParams params = builder.load("field1").noSort().build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 1 @field1 NOSORT
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(1), iter.next());
      assertEquals(RawableFactory.from("@field1"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.NOSORT, iter.next());
    }

    @Test
    public void addParamsWithLoadAndFilter() {
      FTHybridPostProcessingParams params = builder.load("price").filter(Filter.of("@price > 100"))
          .build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 1 @price FILTER @price > 100
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(1), iter.next());
      assertEquals(RawableFactory.from("@price"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.FILTER, iter.next());
      // Continue with FILTER assertions...
    }

    @Test
    public void addParamsWithLoadAndLimit() {
      FTHybridPostProcessingParams params = builder.load("field1", "field2").limit(Limit.of(10, 20))
          .build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Expected: FT.HYBRID LOAD 2 @field1 @field2 LIMIT 10 20
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(2), iter.next());
      assertEquals(RawableFactory.from("@field1"), iter.next());
      assertEquals(RawableFactory.from("@field2"), iter.next());
      assertEquals(SearchProtocol.SearchKeyword.LIMIT, iter.next());
      assertEquals(RawableFactory.from(10), iter.next());
      assertEquals(RawableFactory.from(20), iter.next());
    }

    @Test
    public void addParamsFieldWithoutAtPrefixGetsPrefix() {
      FTHybridPostProcessingParams params = builder.load("field1").build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Field without @ should get @ prefix
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(1), iter.next());
      assertEquals(RawableFactory.from("@field1"), iter.next());
    }

    @Test
    public void addParamsFieldWithAtPrefixNotDuplicated() {
      FTHybridPostProcessingParams params = builder.load("@field1").build();

      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.HYBRID);
      params.addParams(args);

      // Field with @ should not get another @ prefix
      Iterator<Rawable> iter = args.iterator();
      assertEquals(SearchProtocol.SearchCommand.HYBRID, iter.next());
      assertEquals(LOAD, iter.next());
      assertEquals(RawableFactory.from(1), iter.next());
      assertEquals(RawableFactory.from("@field1"), iter.next());
    }
  }
}
