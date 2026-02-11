package redis.clients.jedis.search.hybrid;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.Apply;
import redis.clients.jedis.search.Filter;
import redis.clients.jedis.search.Limit;
import redis.clients.jedis.search.aggr.Group;
import redis.clients.jedis.search.aggr.SortedField;
import redis.clients.jedis.util.JedisAsserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.GROUPBY;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.LOAD;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NOSORT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SORTBY;

/**
 * Arguments for post-processing operations in FT.HYBRID command. Supports LOAD, GROUPBY, APPLY,
 * SORTBY, FILTER, and LIMIT operations.
 * <p>
 * Operations are applied in a specific order:
 * <ol>
 * <li>LOAD - fields to load</li>
 * <li>GROUPBY - grouping with reducers</li>
 * <li>APPLY - computed fields</li>
 * <li>SORTBY - sorting</li>
 * <li>FILTER - filtering results</li>
 * <li>LIMIT - pagination</li>
 * </ol>
 */
@Experimental
public class FTHybridPostProcessingParams implements IParams {

  private List<String> loadFields;
  private boolean loadAll;
  private Group groupBy;
  private final List<Apply> applies = new ArrayList<>();
  private SortedField[] sortByFields;
  private boolean noSort;
  private Filter filter;
  private Limit limit;

  private static final String LOAD_ALL = "*";

  private FTHybridPostProcessingParams() {
  }

  /**
   * @return a new {@link Builder} for {@link FTHybridPostProcessingParams}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link FTHybridPostProcessingParams}.
   */
  public static class Builder {
    private final FTHybridPostProcessingParams instance = new FTHybridPostProcessingParams();

    /**
     * Build the {@link FTHybridPostProcessingParams} instance.
     * @return the configured arguments
     */
    public FTHybridPostProcessingParams build() {
      return instance;
    }

    /**
     * Set the fields to load in the results.
     * <p>
     * This method replaces any previous load configuration (including loadAll()). To load all
     * fields, use {@link #loadAll()} instead.
     * @param fields the field names to load (must not be empty)
     * @return this builder
     * @throws IllegalArgumentException if fields is null, empty, or contains "*"
     */
    public Builder load(String... fields) {
      JedisAsserts.notNull(fields, "Fields must not be null");
      JedisAsserts.isTrue(fields.length > 0, "At least one field is required");

      // Validate no wildcards in specific field list
      for (String field : fields) {
        JedisAsserts.notNull(field, "Field names cannot be null");
        JedisAsserts.isFalse(LOAD_ALL.equals(field),
          "Cannot use '*' in load(). Use loadAll() instead to load all fields.");
      }

      // Clear previous state and set new values
      instance.loadAll = false;
      instance.loadFields = Arrays.asList(fields);
      return this;
    }

    /**
     * Set to load all fields in the results using LOAD *.
     * <p>
     * This method replaces any previous load configuration (including specific fields).
     * <p>
     * Note: requires Redis version &gt;= 8.6.0
     * @return this builder
     */
    public Builder loadAll() {
      instance.loadAll = true;
      instance.loadFields = null;
      return this;
    }

    /**
     * Add a GROUPBY operation using {@link Group} from the aggregation package.
     * @param group the group operation with reducers
     * @return this builder
     */
    public Builder groupBy(Group group) {
      instance.groupBy = group;
      return this;
    }

    /**
     * Add an APPLY operation.
     * @param apply the apply operation
     * @return this builder
     */
    public Builder apply(Apply apply) {
      instance.applies.add(apply);
      return this;
    }

    /**
     * Add a SORTBY operation using {@link SortedField} from the aggregation package.
     * @param fields the sorted fields
     * @return this builder
     */
    public Builder sortBy(SortedField... fields) {
      instance.sortByFields = fields;
      return this;
    }

    /**
     * Disable the default sorting by score. This adds the NOSORT keyword to the command.
     * <p>
     * Note: Cannot be used together with {@link #sortBy(SortedField...)}.
     * @return this builder
     */
    public Builder noSort() {
      instance.noSort = true;
      return this;
    }

    /**
     * Add a FILTER operation.
     * @param filter the filter operation
     * @return this builder
     */
    public Builder filter(Filter filter) {
      instance.filter = filter;
      return this;
    }

    /**
     * Add a LIMIT operation.
     * @param limit the limit operation
     * @return this builder
     */
    public Builder limit(Limit limit) {
      instance.limit = limit;
      return this;
    }
  }

  @Override
  public void addParams(CommandArguments args) {
    // LOAD clause
    if (loadAll || (loadFields != null && !loadFields.isEmpty())) {
      args.add(LOAD);
      if (loadAll) {
        // Special case for LOAD *
        args.add(LOAD_ALL);
      } else {
        args.add(loadFields.size());
        for (String field : loadFields) {
          // Add @ prefix if not already present
          if (!field.startsWith("@")) {
            args.add("@" + field);
          } else {
            args.add(field);
          }
        }
      }
    }

    // GROUPBY - convert aggr.Group to CommandArguments
    if (groupBy != null) {
      List<Object> groupArgs = new ArrayList<>();
      groupBy.addArgs(groupArgs);
      args.add(GROUPBY);
      args.addObjects(groupArgs);
    }

    for (Apply apply : applies) {
      apply.addParams(args);
    }

    // SORTBY or NOSORT - mutually exclusive
    if (noSort) {
      args.add(NOSORT);
    } else if (sortByFields != null && sortByFields.length > 0) {
      args.add(SORTBY);
      args.add(sortByFields.length * 2);
      for (SortedField field : sortByFields) {
        args.add(field.getField());
        args.add(field.getOrder());
      }
    }

    if (filter != null) {
      filter.addParams(args);
    }

    if (limit != null) {
      limit.addParams(args);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FTHybridPostProcessingParams that = (FTHybridPostProcessingParams) o;
    return loadAll == that.loadAll && noSort == that.noSort
        && Objects.equals(loadFields, that.loadFields) && Objects.equals(groupBy, that.groupBy)
        && Objects.equals(applies, that.applies) && Arrays.equals(sortByFields, that.sortByFields)
        && Objects.equals(filter, that.filter) && Objects.equals(limit, that.limit);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(loadFields, loadAll, groupBy, applies, noSort, filter, limit);
    result = 31 * result + Arrays.hashCode(sortByFields);
    return result;
  }
}
