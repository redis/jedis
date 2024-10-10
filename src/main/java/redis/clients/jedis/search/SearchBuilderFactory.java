package redis.clients.jedis.search;

import static redis.clients.jedis.BuilderFactory.STRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.DoublePrecision;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

public final class SearchBuilderFactory {

  public static final Builder<Map<String, Object>> SEARCH_PROFILE_PROFILE = new Builder<Map<String, Object>>() {

    private final String ITERATORS_PROFILE_STR = "Iterators profile";
    private final String CHILD_ITERATORS_STR = "Child iterators";
    private final String RESULT_PROCESSORS_PROFILE_STR = "Result processors profile";

    @Override
    public Map<String, Object> build(Object data) {
      List<Object> list = (List<Object>) SafeEncoder.encodeObject(data);
      Map<String, Object> profileMap = new HashMap<>(list.size(), 1f);

      for (Object listObject : list) {
        List<Object> attributeList = (List<Object>) listObject;
        String attributeName = (String) attributeList.get(0);
        Object attributeValue;

        if (attributeList.size() == 2) {

          Object value = attributeList.get(1);
          if (attributeName.equals(ITERATORS_PROFILE_STR)) {
            attributeValue = parseIterators(value);
          } else if (attributeName.endsWith(" time")) {
            attributeValue = DoublePrecision.parseEncodedFloatingPointNumber(value);
          } else {
            attributeValue = value;
          }

        } else if (attributeList.size() > 2) {

          if (attributeName.equals(RESULT_PROCESSORS_PROFILE_STR)) {
            List<Map<String, Object>> resultProcessorsProfileList = new ArrayList<>(attributeList.size() - 1);
            for (int i = 1; i < attributeList.size(); i++) {
              resultProcessorsProfileList.add(parseResultProcessors(attributeList.get(i)));
            }
            attributeValue = resultProcessorsProfileList;
          } else {
            attributeValue = attributeList.subList(1, attributeList.size());
          }

        } else {
          attributeValue = null;
        }

        profileMap.put(attributeName, attributeValue);
      }
      return profileMap;
    }

    private Map<String, Object> parseResultProcessors(Object data) {
      List<Object> list = (List<Object>) data;
      Map<String, Object> map = new HashMap<>(list.size() / 2, 1f);
      for (int i = 0; i < list.size(); i += 2) {
        String key = (String) list.get(i);
        Object value = list.get(i + 1);
        if (key.equals("Time")) {
          value = DoublePrecision.parseEncodedFloatingPointNumber(value);
        }
        map.put(key, value);
      }
      return map;
    }

    private Object parseIterators(Object data) {
      if (!(data instanceof List)) return data;
      List iteratorsAttributeList = (List) data;
      int childIteratorsIndex = iteratorsAttributeList.indexOf(CHILD_ITERATORS_STR);
      // https://github.com/RediSearch/RediSearch/issues/3205 patch. TODO: Undo if resolved in RediSearch.
      if (childIteratorsIndex < 0) childIteratorsIndex = iteratorsAttributeList.indexOf("Child iterator");

      Map<String, Object> iteratorsProfile;
      if (childIteratorsIndex < 0) {
        childIteratorsIndex = iteratorsAttributeList.size();
        iteratorsProfile = new HashMap<>(childIteratorsIndex / 2, 1f);
      } else {
        iteratorsProfile = new HashMap<>(1 + childIteratorsIndex / 2, 1f);
      }

      for (int i = 0; i < childIteratorsIndex; i += 2) {
        String key = (String) iteratorsAttributeList.get(i);
        Object value = iteratorsAttributeList.get(i + 1);
        if (key.equals("Time")) {
          value = DoublePrecision.parseEncodedFloatingPointNumber(value);
        }
        iteratorsProfile.put(key, value);
      }

      if (childIteratorsIndex + 1 < iteratorsAttributeList.size()) {
        List childIteratorsList = new ArrayList(iteratorsAttributeList.size() - childIteratorsIndex - 1);
        for (int i = childIteratorsIndex + 1; i < iteratorsAttributeList.size(); i++) {
          childIteratorsList.add(parseIterators(iteratorsAttributeList.get(i)));
        }
        iteratorsProfile.put(CHILD_ITERATORS_STR, childIteratorsList);
      }
      return iteratorsProfile;
    }
  };

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
