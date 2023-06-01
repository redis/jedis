package redis.clients.jedis.search.aggr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;
import redis.clients.jedis.util.LazyRawable;

/**
 * @author Guy Korland
 */
public class AggregationBuilder {

  private final List<Object> args = new ArrayList<>();
  private Integer dialect;
  private boolean isWithCursor = false;

  public AggregationBuilder(String query) {
    args.add(query);
  }

  public AggregationBuilder() {
    this("*");
  }

  public AggregationBuilder load(String... fields) {
    return load(FieldName.convert(fields));
  }

  public AggregationBuilder load(FieldName... fields) {
    args.add(SearchKeyword.LOAD);
    LazyRawable rawLoadCount = new LazyRawable();
    args.add(rawLoadCount);
    int loadCount = 0;
    for (FieldName fn : fields) {
      loadCount += fn.addCommandArguments(args);
    }
    rawLoadCount.setRaw(Protocol.toByteArray(loadCount));
    return this;
  }

  public AggregationBuilder loadAll() {
    args.add(SearchKeyword.LOAD);
    args.add(Protocol.BYTES_ASTERISK);
    return this;
  }

  public AggregationBuilder limit(int offset, int count) {
    args.add(SearchKeyword.LIMIT);
    args.add(offset);
    args.add(count);
    return this;
  }

  public AggregationBuilder limit(int count) {
    return limit(0, count);
  }

  public AggregationBuilder sortBy(SortedField... fields) {
    args.add(SearchKeyword.SORTBY);
    args.add(Integer.toString(fields.length * 2));
    for (SortedField field : fields) {
      args.add(field.getField());
      args.add(field.getOrder());
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
    args.add(SearchKeyword.MAX);
    args.add(max);
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
    args.add(SearchKeyword.APPLY);
    args.add(projection);
    args.add(SearchKeyword.AS);
    args.add(alias);
    return this;
  }

  public AggregationBuilder groupBy(Group group) {
    args.add(SearchKeyword.GROUPBY);
    group.addArgs(args);
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
    args.add(SearchKeyword.FILTER);
    args.add(expression);
    return this;
  }

  public AggregationBuilder cursor(int count) {
    isWithCursor = true;
    args.add(SearchKeyword.WITHCURSOR);
    args.add(SearchKeyword.COUNT);
    args.add(count);
    return this;
  }

  public AggregationBuilder cursor(int count, long maxIdle) {
    isWithCursor = true;
    args.add(SearchKeyword.WITHCURSOR);
    args.add(SearchKeyword.COUNT);
    args.add(count);
    args.add(SearchKeyword.MAXIDLE);
    args.add(maxIdle);
    return this;
  }

  public AggregationBuilder verbatim() {
    args.add(SearchKeyword.VERBATIM);
    return this;
  }

  public AggregationBuilder timeout(long timeout) {
    args.add(SearchKeyword.TIMEOUT);
    args.add(timeout);
    return this;
  }

  public AggregationBuilder params(Map<String, Object> params) {
    args.add(SearchKeyword.PARAMS);
    args.add(params.size() * 2);
    params.forEach((k, v) -> {
      args.add(k);
      args.add(v);
    });
    return this;
  }

  public AggregationBuilder dialect(int dialect) {
    this.dialect = dialect;
    return this;
  }

  public List<Object> getArgs() {
    if (dialect != null) {
      args.add(SearchKeyword.DIALECT);
      args.add(dialect);
    }
    return Collections.unmodifiableList(args);
  }

  public boolean isWithCursor() {
    return isWithCursor;
  }
}
