package redis.clients.jedis.timeseries;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

  public static final Builder<Map<String, TSMRangeElements>> TIMESERIES_MRANGE_RESPONSE
      = new Builder<Map<String, TSMRangeElements>>() {
    @Override
    public Map<String, TSMRangeElements> build(Object data) {
      return ((List<Object>) data).stream().map((tsObject) -> (List<Object>) tsObject)
          .map((tsList) -> new TSMRangeElements(BuilderFactory.STRING.build(tsList.get(0)),
              BuilderFactory.STRING_MAP_FROM_PAIRS.build(tsList.get(1)),
              TIMESERIES_ELEMENT_LIST.build(tsList.get(2))))
          .collect(Collectors.toMap(TSMRangeElements::getKey, Function.identity(),
              (x, y) -> x, LinkedHashMap::new));
    }
  };

  public static final Builder<Map<String, TSMRangeElements>> TIMESERIES_MRANGE_RESPONSE_RESP3
      = new Builder<Map<String, TSMRangeElements>>() {
    @Override
    public Map<String, TSMRangeElements> build(Object data) {
      List<Object> dataList = (List<Object>) data;
      Map<String, TSMRangeElements> map = new LinkedHashMap<>(dataList.size() / 2, 1f);
      for (Iterator<Object> iterator = dataList.iterator(); iterator.hasNext();) {
        String key = BuilderFactory.STRING.build(iterator.next());
        List<Object> valueList = (List<Object>) iterator.next();
        TSMRangeElements elements;
        switch (valueList.size()) {
          case 3:
            List<Object> aggrMapObj = (List<Object>) valueList.get(1);
            assert "aggregators".equalsIgnoreCase(BuilderFactory.STRING.build(aggrMapObj.get(0)));
            elements = new TSMRangeElements(key,
                BuilderFactory.STRING_MAP.build(valueList.get(0)),
                ((List<Object>) aggrMapObj.get(1)).stream().map(BuilderFactory.STRING::build)
                    .map(AggregationType::safeValueOf).collect(Collectors.toList()),
                TIMESERIES_ELEMENT_LIST.build(valueList.get(2)));
            break;
          case 4:
            List<Object> rdcMapObj = (List<Object>) valueList.get(1);
            assert "reducers".equalsIgnoreCase(BuilderFactory.STRING.build(rdcMapObj.get(0)));
            List<Object> srcMapObj = (List<Object>) valueList.get(2);
            assert "sources".equalsIgnoreCase(BuilderFactory.STRING.build(srcMapObj.get(0)));
            elements = new TSMRangeElements(key,
                BuilderFactory.STRING_MAP.build(valueList.get(0)),
                BuilderFactory.STRING_LIST.build(rdcMapObj.get(1)),
                BuilderFactory.STRING_LIST.build(srcMapObj.get(1)),
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

  public static final Builder<Map<String, TSMGetElement>> TIMESERIES_MGET_RESPONSE
      = new Builder<Map<String, TSMGetElement>>() {
    @Override
    public Map<String, TSMGetElement> build(Object data) {
      return ((List<Object>) data).stream().map((tsObject) -> (List<Object>) tsObject)
          .map((tsList) -> new TSMGetElement(BuilderFactory.STRING.build(tsList.get(0)),
              BuilderFactory.STRING_MAP_FROM_PAIRS.build(tsList.get(1)),
              TIMESERIES_ELEMENT.build(tsList.get(2))))
          .collect(Collectors.toMap(TSMGetElement::getKey, Function.identity()));
    }
  };

  public static final Builder<Map<String, TSMGetElement>> TIMESERIES_MGET_RESPONSE_RESP3
      = new Builder<Map<String, TSMGetElement>>() {
    @Override
    public Map<String, TSMGetElement> build(Object data) {
      List<Object> dataList = (List<Object>) data;
      Map<String, TSMGetElement> map = new LinkedHashMap<>(dataList.size());
      for (Iterator<Object> iterator = dataList.iterator(); iterator.hasNext();) {
        String key = BuilderFactory.STRING.build(iterator.next());
        List<Object> valueList = (List<Object>) iterator.next();
        TSMGetElement value = new TSMGetElement(key,
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
