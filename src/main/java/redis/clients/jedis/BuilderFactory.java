package redis.clients.jedis;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.resps.LCSMatchResult.MatchedPosition;
import redis.clients.jedis.resps.LCSMatchResult.Position;
import redis.clients.jedis.util.DoublePrecision;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

public final class BuilderFactory {

  public static final Builder<Object> RAW_OBJECT = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      return data;
    }

    @Override
    public String toString() {
      return "Object";
    }
  };

  public static final Builder<List<Object>> RAW_OBJECT_LIST = new Builder<List<Object>>() {
    @Override
    public List<Object> build(Object data) {
      return (List<Object>) data;
    }

    @Override
    public String toString() {
      return "List<Object>";
    }
  };

  public static final Builder<Object> ENCODED_OBJECT = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      return SafeEncoder.encodeObject(data);
    }

    @Override
    public String toString() {
      return "Object";
    }
  };

  public static final Builder<List<Object>> ENCODED_OBJECT_LIST = new Builder<List<Object>>() {
    @Override
    public List<Object> build(Object data) {
      return (List<Object>) SafeEncoder.encodeObject(data);
    }

    @Override
    public String toString() {
      return "List<Object>";
    }
  };

  public static final Builder<Long> LONG = new Builder<Long>() {
    @Override
    public Long build(Object data) {
      return (Long) data;
    }

    @Override
    public String toString() {
      return "Long";
    }

  };

  public static final Builder<List<Long>> LONG_LIST = new Builder<List<Long>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Long> build(Object data) {
      if (null == data) {
        return null;
      }
      return (List<Long>) data;
    }

    @Override
    public String toString() {
      return "List<Long>";
    }

  };

  public static final Builder<Double> DOUBLE = new Builder<Double>() {
    @Override
    public Double build(Object data) {
      if (data == null) return null;
      else if (data instanceof Double) return (Double) data;
      else return DoublePrecision.parseFloatingPointNumber(STRING.build(data));
    }

    @Override
    public String toString() {
      return "Double";
    }
  };

  public static final Builder<List<Double>> DOUBLE_LIST = new Builder<List<Double>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Double> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(DOUBLE::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<Double>";
    }
  };

  public static final Builder<Boolean> BOOLEAN = new Builder<Boolean>() {
    @Override
    public Boolean build(Object data) {
      if (data == null) return null;
      else if (data instanceof Boolean) return (Boolean) data;
      return ((Long) data) == 1L;
    }

    @Override
    public String toString() {
      return "Boolean";
    }
  };

  public static final Builder<List<Boolean>> BOOLEAN_LIST = new Builder<List<Boolean>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Boolean> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(BOOLEAN::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<Boolean>";
    }
  };

  public static final Builder<List<Boolean>> BOOLEAN_WITH_ERROR_LIST = new Builder<List<Boolean>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Boolean> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream()
          //.map((val) -> (val instanceof JedisDataException) ? val : BOOLEAN.build(val))
          .map((val) -> (val instanceof JedisDataException) ? null : BOOLEAN.build(val))
          .collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<Boolean>";
    }
  };

  public static final Builder<byte[]> BINARY = new Builder<byte[]>() {
    @Override
    public byte[] build(Object data) {
      return (byte[]) data;
    }

    @Override
    public String toString() {
      return "byte[]";
    }
  };

  public static final Builder<List<byte[]>> BINARY_LIST = new Builder<List<byte[]>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<byte[]> build(Object data) {
      return (List<byte[]>) data;
    }

    @Override
    public String toString() {
      return "List<byte[]>";
    }
  };

  public static final Builder<Set<byte[]>> BINARY_SET = new Builder<Set<byte[]>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<byte[]> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = BINARY_LIST.build(data);
      return SetFromList.of(l);
    }

    @Override
    public String toString() {
      return "Set<byte[]>";
    }
  };

  public static final Builder<List<Map.Entry<byte[], byte[]>>> BINARY_PAIR_LIST
      = new Builder<List<Map.Entry<byte[], byte[]>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<byte[], byte[]>> build(Object data) {
      final List<byte[]> flatHash = (List<byte[]>) data;
      final List<Map.Entry<byte[], byte[]>> pairList = new ArrayList<>();
      final Iterator<byte[]> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        pairList.add(new AbstractMap.SimpleEntry<>(iterator.next(), iterator.next()));
      }

      return pairList;
    }

    @Override
    public String toString() {
      return "List<Map.Entry<byte[], byte[]>>";
    }
  };

  public static final Builder<List<Map.Entry<byte[], byte[]>>> BINARY_PAIR_LIST_FROM_PAIRS
      = new Builder<List<Map.Entry<byte[], byte[]>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<byte[], byte[]>> build(Object data) {
      final List<Object> list = (List<Object>) data;
      final List<Map.Entry<byte[], byte[]>> pairList = new ArrayList<>();
      for (Object object : list) {
        final List<byte[]> flat = (List<byte[]>) object;
        pairList.add(new AbstractMap.SimpleEntry<>(flat.get(0), flat.get(1)));
      }

      return pairList;
    }

    @Override
    public String toString() {
      return "List<Map.Entry<byte[], byte[]>>";
    }
  };

  public static final Builder<String> STRING = new Builder<String>() {
    @Override
    public String build(Object data) {
      return data == null ? null : SafeEncoder.encode((byte[]) data);
    }

    @Override
    public String toString() {
      return "String";
    }
  };

  public static final Builder<List<String>> STRING_LIST = new Builder<List<String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<String> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(STRING::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<String>";
    }
  };

  public static final Builder<Set<String>> STRING_SET = new Builder<Set<String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(STRING::build).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
      return "Set<String>";
    }
  };

  public static final Builder<Map<byte[], byte[]>> BINARY_MAP = new Builder<Map<byte[], byte[]>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<byte[], byte[]> build(Object data) {
      final List<Object> list = (List<Object>) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        final Map<byte[], byte[]> map = new JedisByteHashMap();
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          KeyValue kv = (KeyValue) iterator.next();
          map.put(BINARY.build(kv.getKey()), BINARY.build(kv.getValue()));
        }
        return map;
      } else {
        final Map<byte[], byte[]> map = new JedisByteHashMap();
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          map.put(BINARY.build(iterator.next()), BINARY.build(iterator.next()));
        }
        return map;
      }
    }

    @Override
    public String toString() {
      return "Map<byte[], byte[]>";
    }
  };

  public static final Builder<Map<String, String>> STRING_MAP = new Builder<Map<String, String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> build(Object data) {
      final List<Object> list = (List<Object>) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        final Map<String, String> map = new HashMap<>(list.size(), 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          KeyValue kv = (KeyValue) iterator.next();
          map.put(STRING.build(kv.getKey()), STRING.build(kv.getValue()));
        }
        return map;
      } else {
        final Map<String, String> map = new HashMap<>(list.size() / 2, 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          map.put(STRING.build(iterator.next()), STRING.build(iterator.next()));
        }
        return map;
      }
    }

    @Override
    public String toString() {
      return "Map<String, String>";
    }
  };

  public static final Builder<Map<String, Object>> ENCODED_OBJECT_MAP = new Builder<Map<String, Object>>() {
    @Override
    public Map<String, Object> build(Object data) {
      if (data == null) return null;
      final List<Object> list = (List<Object>) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        final Map<String, Object> map = new HashMap<>(list.size(), 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          KeyValue kv = (KeyValue) iterator.next();
          map.put(STRING.build(kv.getKey()), ENCODED_OBJECT.build(kv.getValue()));
        }
        return map;
      } else {
        final Map<String, Object> map = new HashMap<>(list.size() / 2, 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          map.put(STRING.build(iterator.next()), ENCODED_OBJECT.build(iterator.next()));
        }
        return map;
      }
    }
  };

  public static final Builder<Object> AGGRESSIVE_ENCODED_OBJECT = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      if (data == null) return null;

      if (data instanceof List) {
        final List list = (List) data;
        if (list.isEmpty()) {
          return list == Protocol.PROTOCOL_EMPTY_MAP ? Collections.emptyMap() : Collections.emptyList();
        }

        if (list.get(0) instanceof KeyValue) {
          return ((List<KeyValue>) data).stream()
              .filter(kv -> kv != null && kv.getKey() != null && kv.getValue() != null)
              .collect(Collectors.toMap(kv -> STRING.build(kv.getKey()),
                  kv -> this.build(kv.getValue())));
        } else {
          return list.stream().map(this::build).collect(Collectors.toList());
        }
      } else if (data instanceof byte[]) {
        return STRING.build(data);
      } else {
        return data;
      }
    }
  };

  public static final Builder<Map<String, Object>> AGGRESSIVE_ENCODED_OBJECT_MAP = new Builder<Map<String, Object>>() {
    @Override
    public Map<String, Object> build(Object data) {
      return (Map<String, Object>) AGGRESSIVE_ENCODED_OBJECT.build(data);
    }
  };

  public static final Builder<List<Map.Entry<String, String>>> STRING_PAIR_LIST
      = new Builder<List<Map.Entry<String, String>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<String, String>> build(Object data) {
      final List<byte[]> flatHash = (List<byte[]>) data;
      final List<Map.Entry<String, String>> pairList = new ArrayList<>(flatHash.size() / 2);
      final Iterator<byte[]> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        pairList.add(KeyValue.of(STRING.build(iterator.next()), STRING.build(iterator.next())));
      }

      return pairList;
    }

    @Override
    public String toString() {
      return "List<Map.Entry<String, String>>";
    }
  };

  public static final Builder<List<Map.Entry<String, String>>> STRING_PAIR_LIST_FROM_PAIRS
      = new Builder<List<Map.Entry<String, String>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<String, String>> build(Object data) {
      return ((List<Object>) data).stream().map(o -> (List<Object>) o)
          .map(l -> KeyValue.of(STRING.build(l.get(0)), STRING.build(l.get(1))))
          .collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<Map.Entry<String, String>>";
    }
  };

  public static final Builder<Map<String, Long>> STRING_LONG_MAP = new Builder<Map<String, Long>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Long> build(Object data) {
      final List<Object> list = (List<Object>) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        final Map<String, Long> map = new LinkedHashMap<>(list.size(), 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          KeyValue kv = (KeyValue) iterator.next();
          map.put(STRING.build(kv.getKey()), LONG.build(kv.getValue()));
        }
        return map;
      } else {
        final Map<String, Long> map = new LinkedHashMap<>(list.size() / 2, 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          map.put(STRING.build(iterator.next()), LONG.build(iterator.next()));
        }
        return map;
      }
    }

    @Override
    public String toString() {
      return "Map<String, Long>";
    }
  };

  public static final Builder<KeyValue<String, String>> KEYED_ELEMENT = new Builder<KeyValue<String, String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<String, String> build(Object data) {
      if (data == null) return null;
      List<Object> l = (List<Object>) data;
      return KeyValue.of(STRING.build(l.get(0)), STRING.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<String, String>";
    }
  };

  public static final Builder<KeyValue<byte[], byte[]>> BINARY_KEYED_ELEMENT = new Builder<KeyValue<byte[], byte[]>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<byte[], byte[]> build(Object data) {
      if (data == null) return null;
      List<Object> l = (List<Object>) data;
      return KeyValue.of(BINARY.build(l.get(0)), BINARY.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<byte[], byte[]>";
    }
  };

  public static final Builder<KeyValue<Long, Double>> ZRANK_WITHSCORE_PAIR = new Builder<KeyValue<Long, Double>>() {
    @Override
    public KeyValue<Long, Double> build(Object data) {
      if (data == null) {
        return null;
      }
      List<Object> l = (List<Object>) data;
      return new KeyValue<>(LONG.build(l.get(0)), DOUBLE.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<Long, Double>";
    }
  };

  public static final Builder<KeyValue<String, List<String>>> KEYED_STRING_LIST
      = new Builder<KeyValue<String, List<String>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<String, List<String>> build(Object data) {
      if (data == null) return null;
      List<byte[]> l = (List<byte[]>) data;
      return new KeyValue<>(STRING.build(l.get(0)), STRING_LIST.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<String, List<String>>";
    }
  };

  public static final Builder<KeyValue<Long, Long>> LONG_LONG_PAIR = new Builder<KeyValue<Long, Long>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<Long, Long> build(Object data) {
      if (data == null) return null;
      List<Object> dataList = (List<Object>) data;
      return new KeyValue<>(LONG.build(dataList.get(0)), LONG.build(dataList.get(1)));
    }
  };

  public static final Builder<List<KeyValue<String, List<String>>>> KEYED_STRING_LIST_LIST
      = new Builder<List<KeyValue<String, List<String>>>>() {
    @Override
    public List<KeyValue<String, List<String>>> build(Object data) {
      List<Object> list = (List<Object>) data;
      return list.stream().map(KEYED_STRING_LIST::build).collect(Collectors.toList());
    }
  };

  public static final Builder<KeyValue<byte[], List<byte[]>>> KEYED_BINARY_LIST
      = new Builder<KeyValue<byte[], List<byte[]>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<byte[], List<byte[]>> build(Object data) {
      if (data == null) return null;
      List<byte[]> l = (List<byte[]>) data;
      return new KeyValue<>(BINARY.build(l.get(0)), BINARY_LIST.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<byte[], List<byte[]>>";
    }
  };

  public static final Builder<Tuple> TUPLE = new Builder<Tuple>() {
    @Override
    @SuppressWarnings("unchecked")
    public Tuple build(Object data) {
      List<byte[]> l = (List<byte[]>) data; // never null
      if (l.isEmpty()) {
        return null;
      }
      return new Tuple(l.get(0), DOUBLE.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "Tuple";
    }
  };

  public static final Builder<KeyValue<String, Tuple>> KEYED_TUPLE = new Builder<KeyValue<String, Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<String, Tuple> build(Object data) {
      if (data == null) return null;
      List<Object> l = (List<Object>) data;
      if (l.isEmpty()) return null;
      return KeyValue.of(STRING.build(l.get(0)), new Tuple(BINARY.build(l.get(1)), DOUBLE.build(l.get(2))));
    }

    @Override
    public String toString() {
      return "KeyValue<String, Tuple>";
    }
  };

  public static final Builder<KeyValue<byte[], Tuple>> BINARY_KEYED_TUPLE = new Builder<KeyValue<byte[], Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<byte[], Tuple> build(Object data) {
      if (data == null) return null;
      List<Object> l = (List<Object>) data;
      if (l.isEmpty()) return null;
      return KeyValue.of(BINARY.build(l.get(0)), new Tuple(BINARY.build(l.get(1)), DOUBLE.build(l.get(2))));
    }

    @Override
    public String toString() {
      return "KeyValue<byte[], Tuple>";
    }
  };

  public static final Builder<List<Tuple>> TUPLE_LIST = new Builder<List<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Tuple> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final List<Tuple> result = new ArrayList<>(l.size() / 2);
      Iterator<byte[]> iterator = l.iterator();
      while (iterator.hasNext()) {
        result.add(new Tuple(iterator.next(), DOUBLE.build(iterator.next())));
      }
      return result;
    }

    @Override
    public String toString() {
      return "List<Tuple>";
    }
  };

  public static final Builder<List<Tuple>> TUPLE_LIST_RESP3 = new Builder<List<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Tuple> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(TUPLE::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<Tuple>";
    }
  };

  @Deprecated
  public static final Builder<Set<Tuple>> TUPLE_ZSET = new Builder<Set<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<Tuple> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<Tuple> result = new LinkedHashSet<>(l.size() / 2, 1);
      Iterator<byte[]> iterator = l.iterator();
      while (iterator.hasNext()) {
        result.add(new Tuple(iterator.next(), DOUBLE.build(iterator.next())));
      }
      return result;
    }

    @Override
    public String toString() {
      return "ZSet<Tuple>";
    }
  };

  @Deprecated
  public static final Builder<Set<Tuple>> TUPLE_ZSET_RESP3 = new Builder<Set<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<Tuple> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(TUPLE::build).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
      return "ZSet<Tuple>";
    }
  };

  private static final Builder<List<Tuple>> TUPLE_LIST_FROM_PAIRS = new Builder<List<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Tuple> build(Object data) {
      if (data == null) return null;
      return ((List<List<Object>>) data).stream().map(TUPLE::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<Tuple>";
    }
  };

  public static final Builder<KeyValue<String, List<Tuple>>> KEYED_TUPLE_LIST
      = new Builder<KeyValue<String, List<Tuple>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<String, List<Tuple>> build(Object data) {
      if (data == null) return null;
      List<Object> l = (List<Object>) data;
      return new KeyValue<>(STRING.build(l.get(0)), TUPLE_LIST_FROM_PAIRS.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<String, List<Tuple>>";
    }
  };

  public static final Builder<KeyValue<byte[], List<Tuple>>> BINARY_KEYED_TUPLE_LIST
      = new Builder<KeyValue<byte[], List<Tuple>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public KeyValue<byte[], List<Tuple>> build(Object data) {
      if (data == null) return null;
      List<Object> l = (List<Object>) data;
      return new KeyValue<>(BINARY.build(l.get(0)), TUPLE_LIST_FROM_PAIRS.build(l.get(1)));
    }

    @Override
    public String toString() {
      return "KeyValue<byte[], List<Tuple>>";
    }
  };

  public static final Builder<ScanResult<String>> SCAN_RESPONSE = new Builder<ScanResult<String>>() {
    @Override
    public ScanResult<String> build(Object data) {
      List<Object> result = (List<Object>) data;
      String newcursor = new String((byte[]) result.get(0));
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      List<String> results = new ArrayList<>(rawResults.size());
      for (byte[] bs : rawResults) {
        results.add(SafeEncoder.encode(bs));
      }
      return new ScanResult<>(newcursor, results);
    }
  };

  public static final Builder<ScanResult<Map.Entry<String, String>>> HSCAN_RESPONSE
      = new Builder<ScanResult<Map.Entry<String, String>>>() {
    @Override
    public ScanResult<Map.Entry<String, String>> build(Object data) {
      List<Object> result = (List<Object>) data;
      String newcursor = new String((byte[]) result.get(0));
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      List<Map.Entry<String, String>> results = new ArrayList<>(rawResults.size() / 2);
      Iterator<byte[]> iterator = rawResults.iterator();
      while (iterator.hasNext()) {
        results.add(new AbstractMap.SimpleEntry<>(SafeEncoder.encode(iterator.next()),
            SafeEncoder.encode(iterator.next())));
      }
      return new ScanResult<>(newcursor, results);
    }
  };

  public static final Builder<ScanResult<String>> SSCAN_RESPONSE = new Builder<ScanResult<String>>() {
    @Override
    public ScanResult<String> build(Object data) {
      List<Object> result = (List<Object>) data;
      String newcursor = new String((byte[]) result.get(0));
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      List<String> results = new ArrayList<>(rawResults.size());
      for (byte[] bs : rawResults) {
        results.add(SafeEncoder.encode(bs));
      }
      return new ScanResult<>(newcursor, results);
    }
  };

  public static final Builder<ScanResult<Tuple>> ZSCAN_RESPONSE = new Builder<ScanResult<Tuple>>() {
    @Override
    public ScanResult<Tuple> build(Object data) {
      List<Object> result = (List<Object>) data;
      String newcursor = new String((byte[]) result.get(0));
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      List<Tuple> results = new ArrayList<>(rawResults.size() / 2);
      Iterator<byte[]> iterator = rawResults.iterator();
      while (iterator.hasNext()) {
        results.add(new Tuple(iterator.next(), BuilderFactory.DOUBLE.build(iterator.next())));
      }
      return new ScanResult<>(newcursor, results);
    }
  };

  public static final Builder<ScanResult<byte[]>> SCAN_BINARY_RESPONSE = new Builder<ScanResult<byte[]>>() {
    @Override
    public ScanResult<byte[]> build(Object data) {
      List<Object> result = (List<Object>) data;
      byte[] newcursor = (byte[]) result.get(0);
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      return new ScanResult<>(newcursor, rawResults);
    }
  };

  public static final Builder<ScanResult<Map.Entry<byte[], byte[]>>> HSCAN_BINARY_RESPONSE
      = new Builder<ScanResult<Map.Entry<byte[], byte[]>>>() {
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> build(Object data) {
      List<Object> result = (List<Object>) data;
      byte[] newcursor = (byte[]) result.get(0);
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      List<Map.Entry<byte[], byte[]>> results = new ArrayList<>(rawResults.size() / 2);
      Iterator<byte[]> iterator = rawResults.iterator();
      while (iterator.hasNext()) {
        results.add(new AbstractMap.SimpleEntry<>(iterator.next(), iterator.next()));
      }
      return new ScanResult<>(newcursor, results);
    }
  };

  public static final Builder<ScanResult<byte[]>> SSCAN_BINARY_RESPONSE = new Builder<ScanResult<byte[]>>() {
    @Override
    public ScanResult<byte[]> build(Object data) {
      List<Object> result = (List<Object>) data;
      byte[] newcursor = (byte[]) result.get(0);
      List<byte[]> rawResults = (List<byte[]>) result.get(1);
      return new ScanResult<>(newcursor, rawResults);
    }
  };

  public static final Builder<Map<String, Long>> PUBSUB_NUMSUB_MAP = new Builder<Map<String, Long>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Long> build(Object data) {
      final List<Object> flatHash = (List<Object>) data;
      final Map<String, Long> hash = new HashMap<>(flatHash.size() / 2, 1f);
      final Iterator<Object> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        hash.put(SafeEncoder.encode((byte[]) iterator.next()), (Long) iterator.next());
      }
      return hash;
    }

    @Override
    public String toString() {
      return "PUBSUB_NUMSUB_MAP<String, String>";
    }
  };

  public static final Builder<List<GeoCoordinate>> GEO_COORDINATE_LIST = new Builder<List<GeoCoordinate>>() {
    @Override
    public List<GeoCoordinate> build(Object data) {
      if (null == data) {
        return null;
      }
      return interpretGeoposResult((List<Object>) data);
    }

    @Override
    public String toString() {
      return "List<GeoCoordinate>";
    }

    private List<GeoCoordinate> interpretGeoposResult(List<Object> responses) {
      List<GeoCoordinate> responseCoordinate = new ArrayList<>(responses.size());
      for (Object response : responses) {
        if (response == null) {
          responseCoordinate.add(null);
        } else {
          List<Object> respList = (List<Object>) response;
          GeoCoordinate coord = new GeoCoordinate(DOUBLE.build(respList.get(0)),
              DOUBLE.build(respList.get(1)));
          responseCoordinate.add(coord);
        }
      }
      return responseCoordinate;
    }
  };

  public static final Builder<List<GeoRadiusResponse>> GEORADIUS_WITH_PARAMS_RESULT = new Builder<List<GeoRadiusResponse>>() {
    @Override
    public List<GeoRadiusResponse> build(Object data) {
      if (data == null) {
        return null;
      }

      List<Object> objectList = (List<Object>) data;

      List<GeoRadiusResponse> responses = new ArrayList<>(objectList.size());
      if (objectList.isEmpty()) {
        return responses;
      }

      if (objectList.get(0) instanceof List<?>) {
        // list of members with additional informations
        GeoRadiusResponse resp;
        for (Object obj : objectList) {
          List<Object> informations = (List<Object>) obj;

          resp = new GeoRadiusResponse((byte[]) informations.get(0));

          int size = informations.size();
          for (int idx = 1; idx < size; idx++) {
            Object info = informations.get(idx);
            if (info instanceof List<?>) {
              // coordinate
              List<Object> coord = (List<Object>) info;

              resp.setCoordinate(new GeoCoordinate(DOUBLE.build(coord.get(0)),
                  DOUBLE.build(coord.get(1))));
            } else if (info instanceof Long) {
              // score
              resp.setRawScore(LONG.build(info));
            } else {
              // distance
              resp.setDistance(DOUBLE.build(info));
            }
          }

          responses.add(resp);
        }
      } else {
        // list of members
        for (Object obj : objectList) {
          responses.add(new GeoRadiusResponse((byte[]) obj));
        }
      }

      return responses;
    }

    @Override
    public String toString() {
      return "GeoRadiusWithParamsResult";
    }
  };

  public static final Builder<Map<String, CommandDocument>> COMMAND_DOCS_RESPONSE = new Builder<Map<String, CommandDocument>>() {
    @Override
    public Map<String, CommandDocument> build(Object data) {
      if (data == null) return null;
      List<Object> list = (List<Object>) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        final Map<String, CommandDocument> map = new HashMap<>(list.size(), 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          KeyValue kv = (KeyValue) iterator.next();
          map.put(STRING.build(kv.getKey()), new CommandDocument(ENCODED_OBJECT_MAP.build(kv.getValue())));
        }
        return map;
      } else {
        final Map<String, CommandDocument> map = new HashMap<>(list.size() / 2, 1f);
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          map.put(STRING.build(iterator.next()), new CommandDocument(ENCODED_OBJECT_MAP.build(iterator.next())));
        }
        return map;
      }
    }
  };

  public static final Builder<Map<String, CommandInfo>> COMMAND_INFO_RESPONSE = new Builder<Map<String, CommandInfo>>() {
    @Override
    public Map<String, CommandInfo> build(Object data) {
      if (data == null) {
        return null;
      }

      List<Object> rawList = (List<Object>) data;
      Map<String, CommandInfo> map = new HashMap<>(rawList.size());

      for (Object rawCommandInfo : rawList) {
        if (rawCommandInfo == null) {
          continue;
        }

        List<Object> commandInfo = (List<Object>) rawCommandInfo;
        String name = STRING.build(commandInfo.get(0));
        CommandInfo info = CommandInfo.COMMAND_INFO_BUILDER.build(commandInfo);
        map.put(name, info);
      }

      return map;
    }
  };

  public static final Builder<Map<String, LatencyLatestInfo>> LATENCY_LATEST_RESPONSE = new Builder<Map<String, LatencyLatestInfo>>() {
    @Override
    public Map<String, LatencyLatestInfo> build(Object data) {
      if (data == null) {
        return null;
      }

      List<Object> rawList = (List<Object>) data;
      Map<String, LatencyLatestInfo> map = new HashMap<>(rawList.size());

      for (Object rawLatencyLatestInfo : rawList) {
        if (rawLatencyLatestInfo == null) {
          continue;
        }

        LatencyLatestInfo latestInfo = LatencyLatestInfo.LATENCY_LATEST_BUILDER.build(rawLatencyLatestInfo);
        String name = latestInfo.getCommand();
        map.put(name, latestInfo);
      }

      return map;
    }
  };

  public static final Builder<List<LatencyHistoryInfo>> LATENCY_HISTORY_RESPONSE = new Builder<List<LatencyHistoryInfo>>() {
    @Override
    public List<LatencyHistoryInfo> build(Object data) {
      if (data == null) {
        return null;
      }

      List<Object> rawList = (List<Object>) data;
      List<LatencyHistoryInfo> response = new ArrayList<>(rawList.size());

      for (Object rawLatencyHistoryInfo : rawList) {
        if (rawLatencyHistoryInfo == null) {
          continue;
        }

        LatencyHistoryInfo historyInfo = LatencyHistoryInfo.LATENCY_HISTORY_BUILDER.build(rawLatencyHistoryInfo);
        response.add(historyInfo);
      }

      return response;
    }
  };

  private static final Builder<List<List<Long>>> CLUSTER_SHARD_SLOTS_RANGES = new Builder<List<List<Long>>>() {

    @Override
    public List<List<Long>> build(Object data) {
      if (null == data) {
        return null;
      }

      List<Long> rawSlots = (List<Long>) data;
      List<List<Long>> slotsRanges = new ArrayList<>();
      for (int i = 0; i < rawSlots.size(); i += 2) {
        slotsRanges.add(Arrays.asList(rawSlots.get(i), rawSlots.get(i + 1)));
      }
      return slotsRanges;
    }
  };

  private static final Builder<List<ClusterShardNodeInfo>> CLUSTER_SHARD_NODE_INFO_LIST
      = new Builder<List<ClusterShardNodeInfo>>() {

    final Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(ClusterShardNodeInfo.ID, STRING);
      tempMappingFunctions.put(ClusterShardNodeInfo.ENDPOINT, STRING);
      tempMappingFunctions.put(ClusterShardNodeInfo.IP, STRING);
      tempMappingFunctions.put(ClusterShardNodeInfo.HOSTNAME, STRING);
      tempMappingFunctions.put(ClusterShardNodeInfo.PORT, LONG);
      tempMappingFunctions.put(ClusterShardNodeInfo.TLS_PORT, LONG);
      tempMappingFunctions.put(ClusterShardNodeInfo.ROLE, STRING);
      tempMappingFunctions.put(ClusterShardNodeInfo.REPLICATION_OFFSET, LONG);
      tempMappingFunctions.put(ClusterShardNodeInfo.HEALTH, STRING);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClusterShardNodeInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<ClusterShardNodeInfo> response = new ArrayList<>();

      List<Object> clusterShardNodeInfos = (List<Object>) data;
      for (Object clusterShardNodeInfoObject : clusterShardNodeInfos) {
        List<Object> clusterShardNodeInfo = (List<Object>) clusterShardNodeInfoObject;
        Iterator<Object> iterator = clusterShardNodeInfo.iterator();
        response.add(new ClusterShardNodeInfo(createMapFromDecodingFunctions(iterator, mappingFunctions)));
      }

      return response;
    }

    @Override
    public String toString() {
      return "List<ClusterShardNodeInfo>";
    }
  };

  public static final Builder<List<ClusterShardInfo>> CLUSTER_SHARD_INFO_LIST
          = new Builder<List<ClusterShardInfo>>() {

    final Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(ClusterShardInfo.SLOTS, CLUSTER_SHARD_SLOTS_RANGES);
      tempMappingFunctions.put(ClusterShardInfo.NODES, CLUSTER_SHARD_NODE_INFO_LIST);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClusterShardInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<ClusterShardInfo> response = new ArrayList<>();

      List<Object> clusterShardInfos = (List<Object>) data;
      for (Object clusterShardInfoObject : clusterShardInfos) {
        List<Object> clusterShardInfo = (List<Object>) clusterShardInfoObject;
        Iterator<Object> iterator = clusterShardInfo.iterator();
        response.add(new ClusterShardInfo(createMapFromDecodingFunctions(iterator, mappingFunctions)));
      }

      return response;
    }

    @Override
    public String toString() {
      return "List<ClusterShardInfo>";
    }
  };

  public static final Builder<List<Module>> MODULE_LIST = new Builder<List<Module>>() {
    @Override
    public List<Module> build(Object data) {
      if (data == null) {
        return null;
      }

      List<List<Object>> objectList = (List<List<Object>>) data;

      List<Module> responses = new ArrayList<>(objectList.size());
      if (objectList.isEmpty()) {
        return responses;
      }

      for (List<Object> moduleResp : objectList) {
        if (moduleResp.get(0) instanceof KeyValue) {
          responses.add(new Module(STRING.build(((KeyValue) moduleResp.get(0)).getValue()),
              LONG.build(((KeyValue) moduleResp.get(1)).getValue()).intValue()));
          continue;
        }
        Module m = new Module(SafeEncoder.encode((byte[]) moduleResp.get(1)),
            ((Long) moduleResp.get(3)).intValue());
        responses.add(m);
      }

      return responses;
    }

    @Override
    public String toString() {
      return "List<Module>";
    }
  };

  /**
   * Create a AccessControlUser object from the ACL GETUSER reply.
   */
  public static final Builder<AccessControlUser> ACCESS_CONTROL_USER = new Builder<AccessControlUser>() {
    @Override
    public AccessControlUser build(Object data) {
      Map<String, Object> map = ENCODED_OBJECT_MAP.build(data);
      if (map == null) return null;
      return new AccessControlUser(map);
    }

    @Override
    public String toString() {
      return "AccessControlUser";
    }
  };

  /**
   * Create an Access Control Log Entry Result of ACL LOG command
   */
  public static final Builder<List<AccessControlLogEntry>> ACCESS_CONTROL_LOG_ENTRY_LIST
      = new Builder<List<AccessControlLogEntry>>() {

    private final Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(AccessControlLogEntry.COUNT, LONG);
      tempMappingFunctions.put(AccessControlLogEntry.REASON, STRING);
      tempMappingFunctions.put(AccessControlLogEntry.CONTEXT, STRING);
      tempMappingFunctions.put(AccessControlLogEntry.OBJECT, STRING);
      tempMappingFunctions.put(AccessControlLogEntry.USERNAME, STRING);
      tempMappingFunctions.put(AccessControlLogEntry.AGE_SECONDS, DOUBLE);
      tempMappingFunctions.put(AccessControlLogEntry.CLIENT_INFO, STRING);
      tempMappingFunctions.put(AccessControlLogEntry.ENTRY_ID, LONG);
      tempMappingFunctions.put(AccessControlLogEntry.TIMESTAMP_CREATED, LONG);
      tempMappingFunctions.put(AccessControlLogEntry.TIMESTAMP_LAST_UPDATED, LONG);

      return tempMappingFunctions;
    }

    @Override
    public List<AccessControlLogEntry> build(Object data) {

      if (null == data) {
        return null;
      }

      List<AccessControlLogEntry> list = new ArrayList<>();
      List<List<Object>> logEntries = (List<List<Object>>) data;
      for (List<Object> logEntryData : logEntries) {
        Iterator<Object> logEntryDataIterator = logEntryData.iterator();
        AccessControlLogEntry accessControlLogEntry = new AccessControlLogEntry(
            createMapFromDecodingFunctions(logEntryDataIterator, mappingFunctions,
                BACKUP_BUILDERS_FOR_DECODING_FUNCTIONS));
        list.add(accessControlLogEntry);
      }
      return list;
    }

    @Override
    public String toString() {
      return "List<AccessControlLogEntry>";
    }
  };

  // Stream Builders -->

  public static final Builder<StreamEntryID> STREAM_ENTRY_ID = new Builder<StreamEntryID>() {
    @Override
    public StreamEntryID build(Object data) {
      if (null == data) {
        return null;
      }
      String id = SafeEncoder.encode((byte[]) data);
      return new StreamEntryID(id);
    }

    @Override
    public String toString() {
      return "StreamEntryID";
    }
  };

  public static final Builder<List<StreamEntryID>> STREAM_ENTRY_ID_LIST = new Builder<List<StreamEntryID>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<StreamEntryID> build(Object data) {
      if (null == data) {
        return null;
      }
      List<Object> objectList = (List<Object>) data;
      List<StreamEntryID> responses = new ArrayList<>(objectList.size());
      if (!objectList.isEmpty()) {
        for(Object object : objectList) {
          responses.add(STREAM_ENTRY_ID.build(object));
        }
      }
      return responses;
    }
  };

  public static final Builder<StreamEntry> STREAM_ENTRY = new Builder<StreamEntry>() {
    @Override
    @SuppressWarnings("unchecked")
    public StreamEntry build(Object data) {
      if (null == data) {
        return null;
      }
      List<Object> objectList = (List<Object>) data;

      if (objectList.isEmpty()) {
        return null;
      }

      String entryIdString = SafeEncoder.encode((byte[]) objectList.get(0));
      StreamEntryID entryID = new StreamEntryID(entryIdString);
      List<byte[]> hash = (List<byte[]>) objectList.get(1);

      Iterator<byte[]> hashIterator = hash.iterator();
      Map<String, String> map = new HashMap<>(hash.size() / 2, 1f);
      while (hashIterator.hasNext()) {
        map.put(SafeEncoder.encode(hashIterator.next()), SafeEncoder.encode(hashIterator.next()));
      }
      return new StreamEntry(entryID, map);
    }

    @Override
    public String toString() {
      return "StreamEntry";
    }
  };

  public static final Builder<List<StreamEntry>> STREAM_ENTRY_LIST = new Builder<List<StreamEntry>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<StreamEntry> build(Object data) {
      if (null == data) {
        return null;
      }
      List<ArrayList<Object>> objectList = (List<ArrayList<Object>>) data;

      List<StreamEntry> responses = new ArrayList<>(objectList.size() / 2);
      if (objectList.isEmpty()) {
        return responses;
      }

      for (ArrayList<Object> res : objectList) {
        if (res == null) {
          responses.add(null);
          continue;
        }
        String entryIdString = SafeEncoder.encode((byte[]) res.get(0));
        StreamEntryID entryID = new StreamEntryID(entryIdString);
        List<byte[]> hash = (List<byte[]>) res.get(1);
        if (hash == null) {
          responses.add(new StreamEntry(entryID, null));
          continue;
        }

        Iterator<byte[]> hashIterator = hash.iterator();
        Map<String, String> map = new HashMap<>(hash.size() / 2, 1f);
        while (hashIterator.hasNext()) {
          map.put(SafeEncoder.encode(hashIterator.next()), SafeEncoder.encode(hashIterator.next()));
        }
        responses.add(new StreamEntry(entryID, map));
      }

      return responses;
    }

    @Override
    public String toString() {
      return "List<StreamEntry>";
    }
  };

  public static final Builder<Map.Entry<StreamEntryID, List<StreamEntry>>> STREAM_AUTO_CLAIM_RESPONSE
      = new Builder<Map.Entry<StreamEntryID, List<StreamEntry>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<StreamEntryID, List<StreamEntry>> build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> objectList = (List<Object>) data;
      return new AbstractMap.SimpleEntry<>(STREAM_ENTRY_ID.build(objectList.get(0)),
          STREAM_ENTRY_LIST.build(objectList.get(1)));
    }

    @Override
    public String toString() {
      return "Map.Entry<StreamEntryID, List<StreamEntry>>";
    }
  };

  public static final Builder<Map.Entry<StreamEntryID, List<StreamEntryID>>> STREAM_AUTO_CLAIM_JUSTID_RESPONSE
      = new Builder<Map.Entry<StreamEntryID, List<StreamEntryID>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map.Entry<StreamEntryID, List<StreamEntryID>> build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> objectList = (List<Object>) data;
      return new AbstractMap.SimpleEntry<>(STREAM_ENTRY_ID.build(objectList.get(0)),
          STREAM_ENTRY_ID_LIST.build(objectList.get(1)));
    }

    @Override
    public String toString() {
      return "Map.Entry<StreamEntryID, List<StreamEntryID>>";
    }
  };

  /**
   * @deprecated Use {@link BuilderFactory#STREAM_AUTO_CLAIM_JUSTID_RESPONSE}.
   */
  @Deprecated
  public static final Builder<Map.Entry<StreamEntryID, List<StreamEntryID>>> STREAM_AUTO_CLAIM_ID_RESPONSE
      = STREAM_AUTO_CLAIM_JUSTID_RESPONSE;

  public static final Builder<List<Map.Entry<String, List<StreamEntry>>>> STREAM_READ_RESPONSE
      = new Builder<List<Map.Entry<String, List<StreamEntry>>>>() {
    @Override
    public List<Map.Entry<String, List<StreamEntry>>> build(Object data) {
      if (data == null) return null;
      List list = (List) data;
      if (list.isEmpty()) return Collections.emptyList();

      if (list.get(0) instanceof KeyValue) {
        return ((List<KeyValue>) list).stream()
            .map(kv -> new KeyValue<>(STRING.build(kv.getKey()),
                STREAM_ENTRY_LIST.build(kv.getValue())))
            .collect(Collectors.toList());
      } else {
        List<Map.Entry<String, List<StreamEntry>>> result = new ArrayList<>(list.size());
        for (Object anObj : list) {
          List<Object> streamObj = (List<Object>) anObj;
          String streamKey = STRING.build(streamObj.get(0));
          List<StreamEntry> streamEntries = STREAM_ENTRY_LIST.build(streamObj.get(1));
          result.add(KeyValue.of(streamKey, streamEntries));
        }
        return result;
      }
    }

    @Override
    public String toString() {
      return "List<Entry<String, List<StreamEntry>>>";
    }
  };

  public static final Builder<Map<String, List<StreamEntry>>> STREAM_READ_MAP_RESPONSE
      = new Builder<Map<String, List<StreamEntry>>>() {
    @Override
    public Map<String, List<StreamEntry>> build(Object data) {
      if (data == null) return null;
      List list = (List) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        return ((List<KeyValue>) list).stream()
            .collect(Collectors.toMap(kv -> STRING.build(kv.getKey()), kv -> STREAM_ENTRY_LIST.build(kv.getValue())));
      } else {
        Map<String, List<StreamEntry>> result = new HashMap<>(list.size());
        for (Object anObj : list) {
          List<Object> streamObj = (List<Object>) anObj;
          String streamKey = STRING.build(streamObj.get(0));
          List<StreamEntry> streamEntries = STREAM_ENTRY_LIST.build(streamObj.get(1));
          result.put(streamKey, streamEntries);
        }
        return result;
      }
    }

    @Override
    public String toString() {
      return "Map<String, List<StreamEntry>>";
    }
  };

  public static final Builder<List<StreamPendingEntry>> STREAM_PENDING_ENTRY_LIST = new Builder<List<StreamPendingEntry>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<StreamPendingEntry> build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> streamsEntries = (List<Object>) data;
      List<StreamPendingEntry> result = new ArrayList<>(streamsEntries.size());
      for (Object streamObj : streamsEntries) {
        List<Object> stream = (List<Object>) streamObj;
        String id = SafeEncoder.encode((byte[]) stream.get(0));
        String consumerName = SafeEncoder.encode((byte[]) stream.get(1));
        long idleTime = BuilderFactory.LONG.build(stream.get(2));
        long deliveredTimes = BuilderFactory.LONG.build(stream.get(3));
        result.add(new StreamPendingEntry(new StreamEntryID(id), consumerName, idleTime,
            deliveredTimes));
      }
      return result;
    }

    @Override
    public String toString() {
      return "List<StreamPendingEntry>";
    }
  };

  public static final Builder<StreamInfo> STREAM_INFO = new Builder<StreamInfo>() {

    Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamInfo.LAST_GENERATED_ID, STREAM_ENTRY_ID);
      tempMappingFunctions.put(StreamInfo.FIRST_ENTRY, STREAM_ENTRY);
      tempMappingFunctions.put(StreamInfo.LENGTH, LONG);
      tempMappingFunctions.put(StreamInfo.RADIX_TREE_KEYS, LONG);
      tempMappingFunctions.put(StreamInfo.RADIX_TREE_NODES, LONG);
      tempMappingFunctions.put(StreamInfo.LAST_ENTRY, STREAM_ENTRY);
      tempMappingFunctions.put(StreamInfo.GROUPS, LONG);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamInfo build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> streamsEntries = (List<Object>) data;
      Iterator<Object> iterator = streamsEntries.iterator();

      return new StreamInfo(createMapFromDecodingFunctions(iterator, mappingFunctions));
    }

    @Override
    public String toString() {
      return "StreamInfo";
    }
  };

  public static final Builder<List<StreamGroupInfo>> STREAM_GROUP_INFO_LIST = new Builder<List<StreamGroupInfo>>() {

    Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamGroupInfo.NAME, STRING);
      tempMappingFunctions.put(StreamGroupInfo.CONSUMERS, LONG);
      tempMappingFunctions.put(StreamGroupInfo.PENDING, LONG);
      tempMappingFunctions.put(StreamGroupInfo.LAST_DELIVERED, STREAM_ENTRY_ID);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StreamGroupInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamGroupInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>) data;
      Iterator<Object> groupsArray = streamsEntries.iterator();

      while (groupsArray.hasNext()) {

        List<Object> groupInfo = (List<Object>) groupsArray.next();

        Iterator<Object> groupInfoIterator = groupInfo.iterator();

        StreamGroupInfo streamGroupInfo = new StreamGroupInfo(createMapFromDecodingFunctions(
          groupInfoIterator, mappingFunctions));
        list.add(streamGroupInfo);

      }
      return list;

    }

    @Override
    public String toString() {
      return "List<StreamGroupInfo>";
    }
  };

  /**
   * @deprecated Use {@link BuilderFactory#STREAM_CONSUMER_INFO_LIST}.
   */
  @Deprecated
  public static final Builder<List<StreamConsumersInfo>> STREAM_CONSUMERS_INFO_LIST
      = new Builder<List<StreamConsumersInfo>>() {

    Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {
      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamConsumersInfo.NAME, STRING);
      tempMappingFunctions.put(StreamConsumersInfo.IDLE, LONG);
      tempMappingFunctions.put(StreamConsumersInfo.PENDING, LONG);
      return tempMappingFunctions;

    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StreamConsumersInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamConsumersInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>) data;
      Iterator<Object> groupsArray = streamsEntries.iterator();

      while (groupsArray.hasNext()) {

        List<Object> groupInfo = (List<Object>) groupsArray.next();

        Iterator<Object> consumerInfoIterator = groupInfo.iterator();

        StreamConsumersInfo streamGroupInfo = new StreamConsumersInfo(
            createMapFromDecodingFunctions(consumerInfoIterator, mappingFunctions));
        list.add(streamGroupInfo);

      }
      return list;

    }

    @Override
    public String toString() {
      return "List<StreamConsumersInfo>";
    }
  };

  public static final Builder<List<StreamConsumerInfo>> STREAM_CONSUMER_INFO_LIST
      = new Builder<List<StreamConsumerInfo>>() {

    Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {
      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamConsumerInfo.NAME, STRING);
      tempMappingFunctions.put(StreamConsumerInfo.IDLE, LONG);
      tempMappingFunctions.put(StreamConsumerInfo.PENDING, LONG);
      return tempMappingFunctions;

    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StreamConsumerInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamConsumerInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>) data;
      Iterator<Object> groupsArray = streamsEntries.iterator();

      while (groupsArray.hasNext()) {

        List<Object> groupInfo = (List<Object>) groupsArray.next();

        Iterator<Object> consumerInfoIterator = groupInfo.iterator();

        StreamConsumerInfo streamConsumerInfo = new StreamConsumerInfo(
            createMapFromDecodingFunctions(consumerInfoIterator, mappingFunctions));
        list.add(streamConsumerInfo);
      }

      return list;
    }

    @Override
    public String toString() {
      return "List<StreamConsumerInfo>";
    }
  };

  private static final Builder<List<StreamConsumerFullInfo>> STREAM_CONSUMER_FULL_INFO_LIST
      = new Builder<List<StreamConsumerFullInfo>>() {

    final Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamConsumerFullInfo.NAME, STRING);
      tempMappingFunctions.put(StreamConsumerFullInfo.SEEN_TIME, LONG);
      tempMappingFunctions.put(StreamConsumerFullInfo.PEL_COUNT, LONG);
      tempMappingFunctions.put(StreamConsumerFullInfo.PENDING, ENCODED_OBJECT_LIST);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StreamConsumerFullInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamConsumerFullInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>) data;

      for (Object streamsEntry : streamsEntries) {
        List<Object> consumerInfoList = (List<Object>) streamsEntry;
        Iterator<Object> consumerInfoIterator = consumerInfoList.iterator();
        StreamConsumerFullInfo consumerInfo = new StreamConsumerFullInfo(
            createMapFromDecodingFunctions(consumerInfoIterator, mappingFunctions));
        list.add(consumerInfo);
      }
      return list;
    }

    @Override
    public String toString() {
      return "List<StreamConsumerFullInfo>";
    }
  };

  private static final Builder<List<StreamGroupFullInfo>> STREAM_GROUP_FULL_INFO_LIST
      = new Builder<List<StreamGroupFullInfo>>() {

    final Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamGroupFullInfo.NAME, STRING);
      tempMappingFunctions.put(StreamGroupFullInfo.CONSUMERS, STREAM_CONSUMER_FULL_INFO_LIST);
      tempMappingFunctions.put(StreamGroupFullInfo.PENDING, ENCODED_OBJECT_LIST);
      tempMappingFunctions.put(StreamGroupFullInfo.LAST_DELIVERED, STREAM_ENTRY_ID);
      tempMappingFunctions.put(StreamGroupFullInfo.PEL_COUNT, LONG);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StreamGroupFullInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamGroupFullInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>) data;

      for (Object streamsEntry : streamsEntries) {

        List<Object> groupInfo = (List<Object>) streamsEntry;

        Iterator<Object> groupInfoIterator = groupInfo.iterator();

        StreamGroupFullInfo groupFullInfo = new StreamGroupFullInfo(
            createMapFromDecodingFunctions(groupInfoIterator, mappingFunctions));
        list.add(groupFullInfo);

      }
      return list;
    }

    @Override
    public String toString() {
      return "List<StreamGroupFullInfo>";
    }
  };

  public static final Builder<StreamFullInfo> STREAM_FULL_INFO = new Builder<StreamFullInfo>() {

    final Map<String, Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String, Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamFullInfo.LAST_GENERATED_ID, STREAM_ENTRY_ID);
      tempMappingFunctions.put(StreamFullInfo.LENGTH, LONG);
      tempMappingFunctions.put(StreamFullInfo.RADIX_TREE_KEYS, LONG);
      tempMappingFunctions.put(StreamFullInfo.RADIX_TREE_NODES, LONG);
      tempMappingFunctions.put(StreamFullInfo.GROUPS, STREAM_GROUP_FULL_INFO_LIST);
      tempMappingFunctions.put(StreamFullInfo.ENTRIES, STREAM_ENTRY_LIST);

      return tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamFullInfo build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> streamsEntries = (List<Object>) data;
      Iterator<Object> iterator = streamsEntries.iterator();

      return new StreamFullInfo(createMapFromDecodingFunctions(iterator, mappingFunctions));
    }

    @Override
    public String toString() {
      return "StreamFullInfo";
    }
  };

  /**
   * @deprecated Use {@link BuilderFactory#STREAM_FULL_INFO}.
   */
  @Deprecated
  public static final Builder<StreamFullInfo> STREAM_INFO_FULL = STREAM_FULL_INFO;

  public static final Builder<StreamPendingSummary> STREAM_PENDING_SUMMARY = new Builder<StreamPendingSummary>() {
    @Override
    @SuppressWarnings("unchecked")
    public StreamPendingSummary build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> objectList = (List<Object>) data;
      long total = LONG.build(objectList.get(0));
      StreamEntryID minId = STREAM_ENTRY_ID.build(objectList.get(1));
      StreamEntryID maxId = STREAM_ENTRY_ID.build(objectList.get(2));
      Map<String, Long> map = objectList.get(3) == null ? null
          : ((List<List<Object>>) objectList.get(3)).stream().collect(
              Collectors.toMap(pair -> STRING.build(pair.get(0)),
                  pair -> Long.parseLong(STRING.build(pair.get(1)))));
      return new StreamPendingSummary(total, minId, maxId, map);
    }

    @Override
    public String toString() {
      return "StreamPendingSummary";
    }
  };

  private static final List<Builder> BACKUP_BUILDERS_FOR_DECODING_FUNCTIONS
      = Arrays.asList(STRING, LONG, DOUBLE);

  private static Map<String, Object> createMapFromDecodingFunctions(Iterator<Object> iterator,
      Map<String, Builder> mappingFunctions) {
    return createMapFromDecodingFunctions(iterator, mappingFunctions, null);
  }

  private static Map<String, Object> createMapFromDecodingFunctions(Iterator<Object> iterator,
      Map<String, Builder> mappingFunctions, Collection<Builder> backupBuilders) {

    if (!iterator.hasNext()) {
      return Collections.emptyMap();
    }

    Map<String, Object> resultMap = new HashMap<>();
    while (iterator.hasNext()) {
      final Object tempObject = iterator.next();
      final String mapKey;
      final Object rawValue;

      if (tempObject instanceof KeyValue) {
        KeyValue kv = (KeyValue) tempObject;
        mapKey = STRING.build(kv.getKey());
        rawValue = kv.getValue();
      } else {
        mapKey = STRING.build(tempObject);
        rawValue = iterator.next();
      }

      if (mappingFunctions.containsKey(mapKey)) {
        resultMap.put(mapKey, mappingFunctions.get(mapKey).build(rawValue));
      } else { // For future - if we don't find an element in our builder map
        Collection<Builder> builders = backupBuilders != null ? backupBuilders : mappingFunctions.values();
        for (Builder b : builders) {
          try {
            resultMap.put(mapKey, b.build(rawValue));
            break;
          } catch (ClassCastException e) {
            // We continue with next builder
          }
        }
      }
    }
    return resultMap;
  }

  // <-- Stream Builders

  public static final Builder<LCSMatchResult> STR_ALGO_LCS_RESULT_BUILDER = new Builder<LCSMatchResult>() {
    @Override
    public LCSMatchResult build(Object data) {
      if (data == null) {
        return null;
      }

      if (data instanceof byte[]) {
        return new LCSMatchResult(STRING.build(data));
      } else if (data instanceof Long) {
        return new LCSMatchResult(LONG.build(data));
      } else {
        long len = 0;
        List<MatchedPosition> matchedPositions = new ArrayList<>();

        List<Object> objectList = (List<Object>) data;
        if (objectList.get(0) instanceof KeyValue) {
          Iterator iterator = objectList.iterator();
          while (iterator.hasNext()) {
            KeyValue kv = (KeyValue) iterator.next();
            if ("matches".equalsIgnoreCase(STRING.build(kv.getKey()))) {
              addMatchedPosition(matchedPositions, kv.getValue());
            } else if ("len".equalsIgnoreCase(STRING.build(kv.getKey()))) {
              len = LONG.build(kv.getValue());
            }
          }
        } else {
          for (int i = 0; i < objectList.size(); i += 2) {
            if ("matches".equalsIgnoreCase(STRING.build(objectList.get(i)))) {
              addMatchedPosition(matchedPositions, objectList.get(i + 1));
            } else if ("len".equalsIgnoreCase(STRING.build(objectList.get(i)))) {
              len = LONG.build(objectList.get(i + 1));
            }
          }
        }

        return new LCSMatchResult(matchedPositions, len);
      }
    }

    private void addMatchedPosition(List<MatchedPosition> matchedPositions, Object o) {
      List<Object> matches = (List<Object>) o;
      for (Object obj : matches) {
        if (obj instanceof List<?>) {
          List<Object> positions = (List<Object>) obj;
          Position a = new Position(
              LONG.build(((List<Object>) positions.get(0)).get(0)),
              LONG.build(((List<Object>) positions.get(0)).get(1))
          );
          Position b = new Position(
              LONG.build(((List<Object>) positions.get(1)).get(0)),
              LONG.build(((List<Object>) positions.get(1)).get(1))
          );
          long matchLen = 0;
          if (positions.size() >= 3) {
            matchLen = LONG.build(positions.get(2));
          }
          matchedPositions.add(new MatchedPosition(a, b, matchLen));
        }
      }
    }
  };

  public static final Builder<Map<String, String>> STRING_MAP_FROM_PAIRS = new Builder<Map<String, String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> build(Object data) {
      final List list = (List) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        return ((List<KeyValue>) list).stream()
            .collect(Collectors.toMap(kv -> STRING.build(kv.getKey()),
                kv -> STRING.build(kv.getValue())));
      }

      final Map<String, String> map = new HashMap<>(list.size());
      for (Object object : list) {
        if (object == null) continue;
        final List<Object> flat = (List<Object>) object;
        if (flat.isEmpty()) continue;
        map.put(STRING.build(flat.get(0)), STRING.build(flat.get(1)));
      }
      return map;
    }

    @Override
    public String toString() {
      return "Map<String, String>";
    }
  };

  public static final Builder<Map<String, Object>> ENCODED_OBJECT_MAP_FROM_PAIRS = new Builder<Map<String, Object>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> build(Object data) {
      final List list = (List) data;
      if (list.isEmpty()) return Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        return ((List<KeyValue>) list).stream()
            .collect(Collectors.toMap(kv -> STRING.build(kv.getKey()),
                kv -> ENCODED_OBJECT.build(kv.getValue())));
      }

      final Map<String, Object> map = new HashMap<>(list.size());
      for (Object object : list) {
        if (object == null) continue;
        final List<Object> flat = (List<Object>) object;
        if (flat.isEmpty()) continue;
        map.put(STRING.build(flat.get(0)), STRING.build(flat.get(1)));
      }
      return map;
    }

    @Override
    public String toString() {
      return "Map<String, String>";
    }
  };

  /**
   * @deprecated Use {@link LibraryInfo#LIBRARY_INFO_LIST}.
   */
  @Deprecated
  public static final Builder<List<LibraryInfo>> LIBRARY_LIST = LibraryInfo.LIBRARY_INFO_LIST;

  public static final Builder<List<List<String>>> STRING_LIST_LIST = new Builder<List<List<String>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<List<String>> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(STRING_LIST::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<List<String>>";
    }
  };

  public static final Builder<List<List<Object>>> ENCODED_OBJECT_LIST_LIST = new Builder<List<List<Object>>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<List<Object>> build(Object data) {
      if (null == data) return null;
      return ((List<Object>) data).stream().map(ENCODED_OBJECT_LIST::build).collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "List<List<Object>>";
    }
  };

  /**
   * A decorator to implement Set from List. Assume that given List do not contains duplicated
   * values. The resulting set displays the same ordering, concurrency, and performance
   * characteristics as the backing list. This class should be used only for Redis commands which
   * return Set result.
   */
  protected static class SetFromList<E> extends AbstractSet<E> implements Serializable {
    private static final long serialVersionUID = -2850347066962734052L;
    private final List<E> list;

    private SetFromList(List<E> list) {
      this.list = list;
    }

    @Override
    public void clear() {
      list.clear();
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return list.contains(o);
    }

    @Override
    public boolean remove(Object o) {
      return list.remove(o);
    }

    @Override
    public boolean add(E e) {
      return !contains(e) && list.add(e);
    }

    @Override
    public Iterator<E> iterator() {
      return list.iterator();
    }

    @Override
    public Object[] toArray() {
      return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return list.toArray(a);
    }

    @Override
    public String toString() {
      return list.toString();
    }

    @Override
    public int hashCode() {
      return list.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) return false;
      if (o == this) return true;
      if (!(o instanceof Set)) return false;

      Collection<?> c = (Collection<?>) o;
      if (c.size() != size()) {
        return false;
      }

      return containsAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return list.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return list.retainAll(c);
    }

    protected static <E> SetFromList<E> of(List<E> list) {
      if (list == null) {
        return null;
      }
      return new SetFromList<>(list);
    }
  }

  private BuilderFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
