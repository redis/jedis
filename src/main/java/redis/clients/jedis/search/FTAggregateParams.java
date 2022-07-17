package redis.clients.jedis.search;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import java.util.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.LazyRaw;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.util.KeyValue;

public class FTAggregateParams implements IParams {

  private boolean verbatim;
  private Collection<String> loadFields;
  private Collection<FieldName> loadFieldNames;
  private boolean loadAll;
  private Long timeout;
  // [ GROUPBY nargs property [property ...] [ REDUCE function nargs arg [arg ...] [AS name] [ REDUCE function nargs arg [arg ...] [AS name] ...]] [ GROUPBY nargs property [property ...] [ REDUCE function nargs arg [arg ...] [AS name] [ REDUCE function nargs arg [arg ...] [AS name] ...]] ...]]
  private final List<KeyValue<String, SortingOrder>> sortByProperties = new LinkedList<>();
  private Integer sortByMax;
  private final List<KeyValue<String, String>> applyExpressions = new LinkedList<>();
  private int[] limit;
  private String filter;
  private boolean withCursor;
  private Integer cursorReadSize;
  private Long cursorIdleTime;
  private Map<String, Object> params;
  private Integer dialect;

  public FTAggregateParams() {
  }

  public static FTAggregateParams aggregateParams() {
    return new FTAggregateParams();
  }

  @Override
  public void addParams(CommandArguments args) {

    if (verbatim) {
      args.add(VERBATIM);
    }

    if (loadAll) {
      args.add(LOAD).add("*");
    } else if (loadFieldNames != null && !loadFieldNames.isEmpty()) {
      args.add(LOAD);
      LazyRaw loadCountObject = new LazyRaw();
      args.add(loadCountObject); // place holder for count
      int argsCount = 0;
      for (FieldName fn : loadFieldNames) {
        argsCount += fn.addCommandArguments(args);
      }
      loadCountObject.setRaw(Protocol.toByteArray(argsCount));
    } else if (loadFields != null && !loadFields.isEmpty()) {
      args.add(LOAD).add(loadFields.size()).addObjects(loadFields);
    }

    if (timeout != null) {
      args.add(TIMEOUT).add(timeout);
    }

    if (!applyExpressions.isEmpty()) {
      applyExpressions.forEach(exp -> args.add(APPLY).add(exp.getKey()).add(AS).add(exp.getValue()));
    }

    if (!sortByProperties.isEmpty()) {
      args.add(SORTBY).add(sortByProperties.size() * 2);
      sortByProperties.forEach(prop -> args.add(prop.getKey()).add(prop.getValue()));
      if (sortByMax != null) {
        args.add(MAX).add(sortByMax);
      }
    }

    if (limit != null) {
      args.add(LIMIT).add(limit[0]).add(limit[1]);
    }

    if (filter != null) {
      args.add(FILTER).add(filter);
    }

    if (withCursor) {
      args.add(WITHCURSOR);
      if (cursorReadSize != null) {
        args.add(COUNT).add(cursorReadSize);
      }
      if (cursorIdleTime != null) {
        args.add(MAXIDLE).add(cursorIdleTime);
      }
    }

    if (params != null && !params.isEmpty()) {
      args.add(PARAMS).add(params.size() * 2);
      params.entrySet().forEach(entry -> args.add(entry.getKey()).add(entry.getValue()));
    }

    if (dialect != null) {
      args.add(DIALECT).add(dialect);
    }
  }

  public FTAggregateParams verbatim() {
    this.verbatim = true;
    return this;
  }

  public FTAggregateParams load(String... fields) {
    if (loadFieldNames != null) {
      Arrays.stream(fields).forEach(f -> loadFieldNames.add(FieldName.of(f)));
    } else {
      if (loadFields == null) {
        loadFields = new ArrayList<>();
      }
      Arrays.stream(fields).forEach(f -> loadFields.add(f));
    }
    return this;
  }

  public FTAggregateParams load(FieldName field) {
    initLoadFieldNames();
    loadFieldNames.add(field);
    return this;
  }

  public FTAggregateParams load(FieldName... fields) {
    return load(Arrays.asList(fields));
  }

  public FTAggregateParams load(Collection<FieldName> fields) {
    initLoadFieldNames();
    loadFieldNames.addAll(fields);
    return this;
  }

  private void initLoadFieldNames() {
    if (loadFieldNames == null) {
      loadFieldNames = new ArrayList<>();
    }
    if (loadFields != null) {
      loadFields.forEach(f -> loadFieldNames.add(FieldName.of(f)));
      loadFields = null;
    }
  }

  public FTAggregateParams loadAll() {
    this.loadAll = true;
    return this;
  }

  public FTAggregateParams timeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  public FTAggregateParams sortByAsc(String property) {
    return sortBy(property, SortingOrder.ASC);
  }

  public FTAggregateParams sortByDesc(String property) {
    return sortBy(property, SortingOrder.DESC);
  }

  public FTAggregateParams sortBy(String property, SortingOrder order) {
    this.sortByProperties.add(KeyValue.of(property, order));
    return this;
  }

  public FTAggregateParams sortByMax(int max) {
    this.sortByMax = max;
    return this;
  }

  public FTAggregateParams apply(String expression, String as) {
    this.applyExpressions.add(KeyValue.of(expression, as));
    return this;
  }

  public FTAggregateParams limit(int offset, int num) {
    this.limit = new int[]{offset, num};
    return this;
  }

  public FTAggregateParams filter(String expression) {
    this.filter = expression;
    return this;
  }

  public FTAggregateParams withCursor() {
    this.withCursor = true;
    return this;
  }

  public FTAggregateParams cursorCount(int readSize) {
    this.cursorReadSize = readSize;
    this.withCursor = true;
    return this;
  }

  public FTAggregateParams cursorMaxIdle(long idleTime) {
    this.cursorIdleTime = idleTime;
    this.withCursor = true;
    return this;
  }

  public FTAggregateParams addParam(String name, Object value) {
    if (params == null) {
      params = new HashMap<>();
    }
    params.put(name, value);
    return this;
  }

  public FTAggregateParams params(Map<String, Object> paramValues) {
    if (this.params == null) {
      this.params = new HashMap<>(paramValues);
    } else {
      this.params.putAll(params);
    }
    return this;
  }

  public FTAggregateParams dialect(int dialect) {
    this.dialect = dialect;
    return this;
  }

  public boolean isWithCursor() {
    return withCursor;
  }
}
