package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;
import redis.clients.jedis.util.LazyRawable;

/**
 * @author Guy Korland
 */
public class AggregationBuilder implements IParams {

  private final List<Object> aggrArgs = new ArrayList<>();
  private Integer dialect;
  private boolean isWithCursor = false;

  public AggregationBuilder(String query) {
    aggrArgs.add(query);
  }

  public AggregationBuilder() {
    this("*");
  }

  public AggregationBuilder load(String... fields) {
    return load(FieldName.convert(fields));
  }

  public AggregationBuilder load(FieldName... fields) {
    aggrArgs.add(SearchKeyword.LOAD);
    LazyRawable rawLoadCount = new LazyRawable();
    aggrArgs.add(rawLoadCount);
    int loadCount = 0;
    for (FieldName fn : fields) {
      loadCount += fn.addCommandArguments(aggrArgs);
    }
    rawLoadCount.setRaw(Protocol.toByteArray(loadCount));
    return this;
  }

  public AggregationBuilder loadAll() {
    aggrArgs.add(SearchKeyword.LOAD);
    aggrArgs.add(Protocol.BYTES_ASTERISK);
    return this;
  }

  public AggregationBuilder limit(int offset, int count) {
    aggrArgs.add(SearchKeyword.LIMIT);
    aggrArgs.add(offset);
    aggrArgs.add(count);
    return this;
  }

  public AggregationBuilder limit(int count) {
    return limit(0, count);
  }

  public AggregationBuilder sortBy(SortedField... fields) {
    aggrArgs.add(SearchKeyword.SORTBY);
    aggrArgs.add(fields.length << 1);
    for (SortedField field : fields) {
      aggrArgs.add(field.getField());
      aggrArgs.add(field.getOrder());
    }
    return this;
  }

  public AggregationBuilder sortByAsc(String field) {
    return sortBy(SortedField.asc(field));
  }

  public AggregationBuilder sortByDesc(String field) {
    return sortBy(SortedField.desc(field));
  }

  /**
   * {@link AggregationBuilder#sortBy(redis.clients.jedis.search.aggr.SortedField...)}
   * (or {@link AggregationBuilder#sortByAsc(java.lang.String)}
   * or {@link AggregationBuilder#sortByDesc(java.lang.String)})
   * MUST BE called JUST BEFORE this.
   * @param max limit
   * @return this
   */
  public AggregationBuilder sortByMax(int max) {
    aggrArgs.add(SearchKeyword.MAX);
    aggrArgs.add(max);
    return this;
  }

  /**
   * Shortcut to {@link AggregationBuilder#sortBy(redis.clients.jedis.search.aggr.SortedField...)}
   * and {@link AggregationBuilder#sortByMax(int)}.
   * @param max limit
   * @param fields sorted fields
   * @return this
   */
  public AggregationBuilder sortBy(int max, SortedField... fields) {
    sortBy(fields);
    sortByMax(max);
    return this;
  }

  public AggregationBuilder apply(String projection, String alias) {
    aggrArgs.add(SearchKeyword.APPLY);
    aggrArgs.add(projection);
    aggrArgs.add(SearchKeyword.AS);
    aggrArgs.add(alias);
    return this;
  }

  public AggregationBuilder groupBy(Group group) {
    aggrArgs.add(SearchKeyword.GROUPBY);
    group.addArgs(aggrArgs);
    return this;
  }

  public AggregationBuilder groupBy(Collection<String> fields, Collection<Reducer> reducers) {
    String[] fieldsArr = new String[fields.size()];
    Group g = new Group(fields.toArray(fieldsArr));
    reducers.forEach((r) -> g.reduce(r));
    groupBy(g);
    return this;
  }

  public AggregationBuilder groupBy(String field, Reducer... reducers) {
    return groupBy(Collections.singletonList(field), Arrays.asList(reducers));
  }

  public AggregationBuilder filter(String expression) {
    aggrArgs.add(SearchKeyword.FILTER);
    aggrArgs.add(expression);
    return this;
  }

  public AggregationBuilder cursor(int count) {
    isWithCursor = true;
    aggrArgs.add(SearchKeyword.WITHCURSOR);
    aggrArgs.add(SearchKeyword.COUNT);
    aggrArgs.add(count);
    return this;
  }

  public AggregationBuilder cursor(int count, long maxIdle) {
    isWithCursor = true;
    aggrArgs.add(SearchKeyword.WITHCURSOR);
    aggrArgs.add(SearchKeyword.COUNT);
    aggrArgs.add(count);
    aggrArgs.add(SearchKeyword.MAXIDLE);
    aggrArgs.add(maxIdle);
    return this;
  }

  public AggregationBuilder verbatim() {
    aggrArgs.add(SearchKeyword.VERBATIM);
    return this;
  }

  public AggregationBuilder timeout(long timeout) {
    aggrArgs.add(SearchKeyword.TIMEOUT);
    aggrArgs.add(timeout);
    return this;
  }

  public AggregationBuilder addScores() {
    aggrArgs.add(SearchKeyword.ADDSCORES);
    return this;
  }

  public AggregationBuilder params(Map<String, Object> params) {
    aggrArgs.add(SearchKeyword.PARAMS);
    aggrArgs.add(params.size() << 1);
    params.forEach((k, v) -> {
      aggrArgs.add(k);
      aggrArgs.add(v);
    });
    return this;
  }

  public AggregationBuilder dialect(int dialect) {
    this.dialect = dialect;
    return this;
  }

  /**
   * This method will not replace the dialect if it has been already set.
   * @param dialect dialect
   * @return this
   */
  public AggregationBuilder dialectOptional(int dialect) {
    if (dialect != 0 && this.dialect == null) {
      this.dialect = dialect;
    }
    return this;
  }

  public boolean isWithCursor() {
    return isWithCursor;
  }

  @Override
  public void addParams(CommandArguments commArgs) {
    commArgs.addObjects(aggrArgs);
    if (dialect != null) {
      commArgs.add(SearchKeyword.DIALECT).add(dialect);
    }
  }
}
