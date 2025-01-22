package redis.clients.jedis.search;

import static redis.clients.jedis.BuilderFactory.STRING;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.KeyValue;

public final class SearchBuilderFactory {

  public static final Builder<Map<String, List<String>>> SEARCH_SYNONYM_GROUPS = new Builder<Map<String, List<String>>>() {
    @Override
    public Map<String, List<String>> build(Object data) {
      List list = (List) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        return ((List<KeyValue>) data).stream().collect(Collectors.toMap(
            kv -> STRING.build(kv.getKey()), kv -> BuilderFactory.STRING_LIST.build(kv.getValue())));
      }

      Map<String, List<String>> dump = new HashMap<>(list.size() / 2, 1f);
      for (int i = 0; i < list.size(); i += 2) {
        dump.put(STRING.build(list.get(i)), BuilderFactory.STRING_LIST.build(list.get(i + 1)));
      }
      return dump;
    }
  };

  public static final Builder<Map<String, Map<String, Double>>> SEARCH_SPELLCHECK_RESPONSE
      = new Builder<Map<String, Map<String, Double>>>() {

    private static final String TERM = "TERM";
    private static final String RESULTS = "results";

    @Override
    public Map<String, Map<String, Double>> build(Object data) {
      List rawDataList = (List) data;
      if (rawDataList.isEmpty()) return Collections.emptyMap();

      if (rawDataList.get(0) instanceof KeyValue) {
        KeyValue rawData = (KeyValue) rawDataList.get(0);
        String header = STRING.build(rawData.getKey());
        if (!RESULTS.equals(header)) {
          throw new IllegalStateException("Unrecognized header: " + header);
        }

        return ((List<KeyValue>) rawData.getValue()).stream().collect(Collectors.toMap(
            rawTerm -> STRING.build(rawTerm.getKey()),
            rawTerm -> ((List<List<KeyValue>>) rawTerm.getValue()).stream()
                .collect(Collectors.toMap(entry -> STRING.build(entry.get(0).getKey()),
                      entry -> BuilderFactory.DOUBLE.build(entry.get(0).getValue()))),
            (x, y) -> x, LinkedHashMap::new));
      }

      Map<String, Map<String, Double>> returnTerms = new LinkedHashMap<>(rawDataList.size());

      for (Object rawData : rawDataList) {
        List<Object> rawElements = (List<Object>) rawData;

        String header = STRING.build(rawElements.get(0));
        if (!TERM.equals(header)) {
          throw new IllegalStateException("Unrecognized header: " + header);
        }
        String term = STRING.build(rawElements.get(1));

        List<List<Object>> list = (List<List<Object>>) rawElements.get(2);
        Map<String, Double> entries = new LinkedHashMap<>(list.size());
        list.forEach(entry -> entries.put(STRING.build(entry.get(1)), BuilderFactory.DOUBLE.build(entry.get(0))));

        returnTerms.put(term, entries);
      }
      return returnTerms;
    }
  };

  private SearchBuilderFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
