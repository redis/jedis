package redis.clients.jedis.search.aggr;

import java.util.*;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.util.SafeEncoder;

/**
 * @author Guy Korland
 */
public class AggregationBuilder {

  private final List<String> args = new ArrayList<>();
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
    args.add("LOAD");
    final int loadCountIndex = args.size();
    args.add(null);
    int loadCount = 0;
    for (FieldName fn : fields) {
      loadCount += fn.addCommandEncodedArguments(args);
    }
    args.set(loadCountIndex, Integer.toString(loadCount));
    return this;
  }

  public AggregationBuilder loadAll() {
    args.add("LOAD");
    args.add("*");
    return this;
  }

  public AggregationBuilder limit(int offset, int count) {
    Limit limit = new Limit(offset, count);
    limit.addArgs(args);
    return this;
  }

  public AggregationBuilder limit(int count) {
    return limit(0, count);
  }

  public AggregationBuilder sortBy(SortedField... fields) {
    args.add("SORTBY");
    args.add(Integer.toString(fields.length * 2));
    for (SortedField field : fields) {
      args.add(field.getField());
      args.add(field.getOrder());
    }

    return this;
  }

  public AggregationBuilder sortBy(int max, SortedField... fields) {
    sortBy(fields);
    if (max > 0) {
      args.add("MAX");
      args.add(Integer.toString(max));
    }
    return this;
  }

  public AggregationBuilder sortByAsc(String field) {
    return sortBy(SortedField.asc(field));
  }

  public AggregationBuilder sortByDesc(String field) {
    return sortBy(SortedField.desc(field));
  }

  public AggregationBuilder apply(String projection, String alias) {
    args.add("APPLY");
    args.add(projection);
    args.add("AS");
    args.add(alias);
    return this;
  }

  public AggregationBuilder groupBy(Collection<String> fields, Collection<Reducer> reducers) {
    String[] fieldsArr = new String[fields.size()];
    Group g = new Group(fields.toArray(fieldsArr));
    for (Reducer r : reducers) {
      g.reduce(r);
    }
    groupBy(g);
    return this;
  }

  public AggregationBuilder groupBy(String field, Reducer... reducers) {
    return groupBy(Collections.singletonList(field), Arrays.asList(reducers));
  }

  public AggregationBuilder groupBy(Group group) {
    args.add("GROUPBY");
    group.addArgs(args);
    return this;
  }

  public AggregationBuilder filter(String expression) {
    args.add("FILTER");
    args.add(expression);
    return this;
  }

  public AggregationBuilder cursor(int count, long maxIdle) {
    isWithCursor = true;
    if (count > 0) {
      args.add("WITHCURSOR");
      args.add("COUNT");
      args.add(Integer.toString(count));
      if (maxIdle < Long.MAX_VALUE && maxIdle >= 0) {
        args.add("MAXIDLE");
        args.add(Long.toString(maxIdle));
      }
    }
    return this;
  }

  public AggregationBuilder verbatim() {
    args.add("VERBATIM");
    return this;
  }

  public AggregationBuilder timeout(long timeout) {
    if (timeout >= 0) {
      args.add("TIMEOUT");
      args.add(Long.toString(timeout));
    }
    return this;
  }

  public AggregationBuilder params(Map<String, Object> params) {
    if (params.size() >= 1) {
      args.add("PARAMS");
      args.add(Integer.toString(params.size() * 2));
      for (Map.Entry<String, Object> entry : params.entrySet()) {
        args.add(entry.getKey());
        args.add(String.valueOf(entry.getValue()));
      }
    }

    return this;
  }

  public AggregationBuilder dialect(int dialect) {
    args.add("DIALECT");
    args.add(Integer.toString(dialect));
    return this;
  }

  public List<String> getArgs() {
    return Collections.unmodifiableList(args);
  }

  public void serializeRedisArgs(List<byte[]> redisArgs) {
    for (String s : getArgs()) {
      redisArgs.add(SafeEncoder.encode(s));
    }
  }

  public String getArgsString() {
    StringJoiner sj = new StringJoiner(" ");
    for (String s : getArgs()) {
      sj.add(s);
    }
    return sj.toString();
  }

  public boolean isWithCursor() {
    return isWithCursor;
  }
}
