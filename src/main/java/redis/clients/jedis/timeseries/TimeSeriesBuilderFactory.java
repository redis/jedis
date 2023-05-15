package redis.clients.jedis.timeseries;

import static java.util.function.Function.identity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.SafeEncoder;

public final class TimeSeriesBuilderFactory {

  public static final Builder<TSElement> TIMESERIES_ELEMENT = new Builder<TSElement>() {
    @Override
    public TSElement build(Object data) {
      List<Object> list = (List<Object>) data;
      if (list == null || list.isEmpty()) return null;
      return new TSElement(BuilderFactory.LONG.build(list.get(0)), BuilderFactory.DOUBLE.build(list.get(1)));
    }
  };

  public static final Builder<List<TSElement>> TIMESERIES_ELEMENT_LIST = new Builder<List<TSElement>>() {
    @Override
    public List<TSElement> build(Object data) {
      return ((List<Object>) data).stream().map((pairObject) -> (List<Object>) pairObject)
          .map((pairList) -> new TSElement(BuilderFactory.LONG.build(pairList.get(0)),
              BuilderFactory.DOUBLE.build(pairList.get(1))))
          .collect(Collectors.toList());
    }
  };

  public static final Builder<Map<String, TSKeyedElements>> TIMESERIES_MRANGE_RESPONSE
      = new Builder<Map<String, TSKeyedElements>>() {
    @Override
    public Map<String, TSKeyedElements> build(Object data) {
      return ((List<Object>) data).stream().map((tsObject) -> (List<Object>) tsObject)
          .map((tsList) -> new TSKeyedElements(BuilderFactory.STRING.build(tsList.get(0)),
              BuilderFactory.STRING_MAP_FROM_PAIRS.build(tsList.get(1)),
              TIMESERIES_ELEMENT_LIST.build(tsList.get(2))))
          .collect(Collectors.toMap(TSKeyedElements::getKey, identity()));
    }
  };

  // TODO:
  public static final Builder<Map<String, TSKeyedElements>> TIMESERIES_MRANGE_RESPONSE_RESP3
      = new Builder<Map<String, TSKeyedElements>>() {
    @Override
    public Map<String, TSKeyedElements> build(Object data) {
      List<Object> dataList = (List<Object>) data;
      Map<String, TSKeyedElements> map = new LinkedHashMap<>(dataList.size() / 2, 1f);
      for (Iterator<Object> iterator = dataList.iterator(); iterator.hasNext();) {
        String key = BuilderFactory.STRING.build(iterator.next());
        List<Object> valueList = (List<Object>) iterator.next();
        TSKeyedElements elements;
        switch (valueList.size()) {
          case 3:
            elements = new TSKeyedElements(key,
                BuilderFactory.STRING_MAP.build(valueList.get(0)),
                // TODO: valueList.get(1)
                TIMESERIES_ELEMENT_LIST.build(valueList.get(2)));
            break;
          case 4:
            elements = new TSKeyedElements(key,
                BuilderFactory.STRING_MAP.build(valueList.get(0)),
                // TODO: valueList.get(1)
                // TODO: valueList.get(2)
                TIMESERIES_ELEMENT_LIST.build(valueList.get(3)));
            break;
          default:
            throw new IllegalStateException();
        }
        map.put(key, elements);
      }
      return map;
    }
  };

  public static final Builder<Map<String, TSKeyValue<TSElement>>> TIMESERIES_MGET_RESPONSE
      = new Builder<Map<String, TSKeyValue<TSElement>>>() {
    @Override
    public Map<String, TSKeyValue<TSElement>> build(Object data) {
      return ((List<Object>) data).stream().map((tsObject) -> (List<Object>) tsObject)
          .map((tsList) -> new TSKeyValue<>(BuilderFactory.STRING.build(tsList.get(0)),
              BuilderFactory.STRING_MAP_FROM_PAIRS.build(tsList.get(1)),
              TIMESERIES_ELEMENT.build(tsList.get(2))))
          .collect(Collectors.toMap(TSKeyValue::getKey, identity()));
    }
  };

  // TODO:
  public static final Builder<Map<String, TSKeyValue<TSElement>>> TIMESERIES_MGET_RESPONSE_RESP3
      = new Builder<Map<String, TSKeyValue<TSElement>>>() {
    @Override
    public Map<String, TSKeyValue<TSElement>> build(Object data) {
      List<Object> dataList = (List<Object>) data;
      Map<String, TSKeyValue<TSElement>> map = new LinkedHashMap<>(dataList.size());
      for (Iterator<Object> iterator = dataList.iterator(); iterator.hasNext();) {
        String key = BuilderFactory.STRING.build(iterator.next());
        List<Object> valueList = (List<Object>) iterator.next();
        TSKeyValue<TSElement> value = new TSKeyValue<>(key,
            BuilderFactory.STRING_MAP.build(valueList.get(0)),
            TIMESERIES_ELEMENT.build(valueList.get(1)));
        map.put(key, value);
      }
      return map;
    }
  };

  private TimeSeriesBuilderFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
