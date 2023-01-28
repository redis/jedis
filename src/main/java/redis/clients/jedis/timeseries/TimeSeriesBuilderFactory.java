package redis.clients.jedis.timeseries;

import java.util.List;
import java.util.stream.Collectors;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;

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

  public static final Builder<List<TSKeyedElements>> TIMESERIES_MRANGE_RESPONSE = new Builder<List<TSKeyedElements>>() {
    @Override
    public List<TSKeyedElements> build(Object data) {
      return ((List<Object>) data).stream().map((tsObject) -> (List<Object>) tsObject)
          .map((tsList) -> new TSKeyedElements(BuilderFactory.STRING.build(tsList.get(0)),
              BuilderFactory.STRING_MAP_FROM_PAIRS.build(tsList.get(1)),
              TIMESERIES_ELEMENT_LIST.build(tsList.get(2))))
          .collect(Collectors.toList());
    }
  };

  public static final Builder<List<TSKeyValue<TSElement>>> TIMESERIES_MGET_RESPONSE
      = new Builder<List<TSKeyValue<TSElement>>>() {
    @Override
    public List<TSKeyValue<TSElement>> build(Object data) {
      return ((List<Object>) data).stream().map((tsObject) -> (List<Object>) tsObject)
          .map((tsList) -> new TSKeyValue<>(BuilderFactory.STRING.build(tsList.get(0)),
              BuilderFactory.STRING_MAP_FROM_PAIRS.build(tsList.get(1)),
              TIMESERIES_ELEMENT.build(tsList.get(2))))
          .collect(Collectors.toList());
    }
  };

  private TimeSeriesBuilderFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
