package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

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
public class PostProcessingParams implements IParams {

  private List<String> loadFields;
  private GroupBy groupBy;
  private final List<Apply> applies = new ArrayList<>();
  private SortBy sortBy;
  private Filter filter;
  private Limit limit;

  private PostProcessingParams() {
  }

  /**
   * @return a new {@link Builder} for {@link PostProcessingParams}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link PostProcessingParams}.
   */
  public static class Builder {
    private final PostProcessingParams instance = new PostProcessingParams();

    /**
     * Build the {@link PostProcessingParams} instance.
     * @return the configured arguments
     */
    public PostProcessingParams build() {
      return instance;
    }

    /**
     * Set the fields to load in the results.
     * @param fields the field names to load
     * @return this builder
     */
    public Builder load(String... fields) {
      instance.loadFields = Arrays.asList(fields);
      return this;
    }

    /**
     * Add a GROUPBY operation.
     * @param groupBy the groupBy operation
     * @return this builder
     */
    public Builder groupBy(GroupBy groupBy) {
      instance.groupBy = groupBy;
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
     * Add a SORTBY operation.
     * @param sortBy the sortBy operation
     * @return this builder
     */
    public Builder sortBy(SortBy sortBy) {
      instance.sortBy = sortBy;
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
    if (loadFields != null && !loadFields.isEmpty()) {
      args.add(LOAD);
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

    // Operations in specific order
    if (groupBy != null) {
      groupBy.addParams(args);
    }

    for (Apply apply : applies) {
      apply.addParams(args);
    }

    if (sortBy != null) {
      sortBy.addParams(args);
    }

    if (filter != null) {
      filter.addParams(args);
    }

    if (limit != null) {
      limit.addParams(args);
    }
  }

  /**
   * GROUPBY operation.
   */
  public static class GroupBy implements IParams {
    private final List<String> fields;
    private final List<Reducer> reducers = new ArrayList<>();

    private GroupBy(String... fields) {
      this.fields = Arrays.asList(fields);
    }

    /**
     * Create a GROUPBY operation.
     * @param fields the fields to group by
     * @return a new GroupBy instance
     */
    public static GroupBy of(String... fields) {
      return new GroupBy(fields);
    }

    /**
     * Add a reducer to this GROUPBY operation.
     * @param reducer the reducer to add
     * @return this GroupBy instance
     */
    public GroupBy reduce(Reducer reducer) {
      this.reducers.add(reducer);
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(GROUPBY);
      args.add(fields.size());
      for (String field : fields) {
        // Add @ prefix if not already present
        if (!field.startsWith("@")) {
          args.add("@" + field);
        } else {
          args.add(field);
        }
      }
      for (Reducer reducer : reducers) {
        reducer.addParams(args);
      }
    }
  }

  /**
   * Reducer for GROUPBY operations.
   */
  public static class Reducer implements IParams {
    private final ReduceFunction function;
    private final List<String> args;
    private String alias;

    private Reducer(ReduceFunction function, String... args) {
      this.function = function;
      this.args = args.length == 0 ? new ArrayList<>() : Arrays.asList(args);
    }

    /**
     * Create a reducer with the specified function and arguments.
     * @param function the reduce function
     * @param args the function arguments
     * @return a new Reducer instance
     */
    public static Reducer of(ReduceFunction function, String... args) {
      return new Reducer(function, args);
    }

    /**
     * Set an alias for the reducer result.
     * @param alias the alias name
     * @return this Reducer instance
     */
    public Reducer as(String alias) {
      this.alias = alias;
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(REDUCE);
      args.add(function.name());
      args.add(this.args.size());
      for (String arg : this.args) {
        args.add(arg);
      }
      if (alias != null) {
        args.add(AS);
        args.add(alias);
      }
    }
  }

  /**
   * Enumeration of REDUCE functions for GROUPBY operations.
   */
  public enum ReduceFunction {
    COUNT, COUNT_DISTINCT, COUNT_DISTINCTISH, SUM, AVG, MIN, MAX, STDDEV, QUANTILE, TOLIST,
    FIRST_VALUE, RANDOM_SAMPLE
  }

  /**
   * SORTBY operation.
   */
  public static class SortBy implements IParams {
    private final List<SortProperty> properties;

    private SortBy(SortProperty... properties) {
      this.properties = Arrays.asList(properties);
    }

    /**
     * Create a SORTBY operation.
     * @param properties the sort properties
     * @return a new SortBy instance
     */
    public static SortBy of(SortProperty... properties) {
      return new SortBy(properties);
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(SORTBY);
      args.add(properties.size() * 2);
      for (SortProperty property : properties) {
        // Add @ prefix if not already present
        String propertyStr = property.property;
        if (!propertyStr.startsWith("@")) {
          args.add("@" + propertyStr);
        } else {
          args.add(propertyStr);
        }
        args.add(property.direction == SortDirection.ASC ? ASC : DESC);
      }
    }
  }

  /**
   * Sort property with direction.
   */
  public static class SortProperty {
    private final String property;
    private final SortDirection direction;

    public SortProperty(String property, SortDirection direction) {
      this.property = property;
      this.direction = direction;
    }
  }

  /**
   * Sort direction enumeration.
   */
  public enum SortDirection {
    ASC, DESC
  }

  /**
   * APPLY operation.
   */
  public static class Apply implements IParams {
    private final String expression;
    private final String alias;

    private Apply(String expression, String alias) {
      this.expression = expression;
      this.alias = alias;
    }

    /**
     * Create an APPLY operation.
     * @param expression the expression to apply
     * @param alias the alias for the result
     * @return a new Apply instance
     */
    public static Apply of(String expression, String alias) {
      return new Apply(expression, alias);
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(APPLY);
      args.add(expression);
      args.add(AS);
      args.add(alias);
    }
  }

  /**
   * FILTER operation.
   */
  public static class Filter implements IParams {
    private final String expression;

    private Filter(String expression) {
      this.expression = expression;
    }

    /**
     * Create a FILTER operation.
     * @param expression the filter expression
     * @return a new Filter instance
     */
    public static Filter of(String expression) {
      return new Filter(expression);
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(FILTER);
      args.add(expression);
    }
  }

  /**
   * LIMIT operation.
   */
  public static class Limit implements IParams {
    private final int offset;
    private final int count;

    private Limit(int offset, int count) {
      this.offset = offset;
      this.count = count;
    }

    /**
     * Create a LIMIT operation.
     * @param offset the offset
     * @param count the count
     * @return a new Limit instance
     */
    public static Limit of(int offset, int count) {
      return new Limit(offset, count);
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(LIMIT);
      args.add(offset);
      args.add(count);
    }
  }
}
