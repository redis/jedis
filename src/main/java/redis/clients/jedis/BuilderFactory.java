package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.util.JedisByteHashMap;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Slowlog;

public class BuilderFactory {
  public static final Builder<Double> DOUBLE = new Builder<Double>() {
    public Double build(Object data) {
      String asString = STRING.build(data);
      return asString == null ? null : Double.valueOf(asString);
    }

    public String toString() {
      return "double";
    }
  };
  public static final Builder<Boolean> BOOLEAN = new Builder<Boolean>() {
    public Boolean build(Object data) {
      return ((Long) data) == 1;
    }

    public String toString() {
      return "boolean";
    }
  };

  public static final Builder<List<Boolean>> BOOLEAN_LIST = new Builder<List<Boolean>>() {
    @SuppressWarnings("unchecked")
    public List<Boolean> build(Object data) {
      if (null == data) {
        return null;
      }
      List<Long> l = (List<Long>) data;
      final ArrayList<Boolean> result = new ArrayList<Boolean>(l.size());
      for (final Long ldata : l) {
        if (ldata == null) {
          result.add(null);
        } else {
          result.add(ldata == 1);
        }
      }
      return result;
    }
  };

  public static final Builder<byte[]> BYTE_ARRAY = new Builder<byte[]>() {
    public byte[] build(Object data) {
      return ((byte[]) data); // deleted == 1
    }

    public String toString() {
      return "byte[]";
    }
  };

  public static final Builder<Long> LONG = new Builder<Long>() {
    public Long build(Object data) {
      return (Long) data;
    }

    public String toString() {
      return "long";
    }

  };
  public static final Builder<String> STRING = new Builder<String>() {
    public String build(Object data) {
      return data == null ? null : SafeEncoder.encode((byte[]) data);
    }

    public String toString() {
      return "string";
    }

  };
  public static final Builder<List<String>> STRING_LIST = new Builder<List<String>>() {
    @SuppressWarnings("unchecked")
    public List<String> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final ArrayList<String> result = new ArrayList<String>(l.size());
      for (final byte[] barray : l) {
        if (barray == null) {
          result.add(null);
        } else {
          result.add(SafeEncoder.encode(barray));
        }
      }
      return result;
    }

    public String toString() {
      return "List<String>";
    }

  };
  public static final Builder<Map<String, String>> STRING_MAP = new Builder<Map<String, String>>() {
    @SuppressWarnings("unchecked")
    public Map<String, String> build(Object data) {
      final List<byte[]> flatHash = (List<byte[]>) data;
      final Map<String, String> hash = new HashMap<String, String>();
      final Iterator<byte[]> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        hash.put(SafeEncoder.encode(iterator.next()), SafeEncoder.encode(iterator.next()));
      }

      return hash;
    }

    public String toString() {
      return "Map<String, String>";
    }

  };

  public static final Builder<Map<String, String>> PUBSUB_NUMSUB_MAP = new Builder<Map<String, String>>() {
    @SuppressWarnings("unchecked")
    public Map<String, String> build(Object data) {
      final List<Object> flatHash = (List<Object>) data;
      final Map<String, String> hash = new HashMap<String, String>();
      final Iterator<Object> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        hash.put(SafeEncoder.encode((byte[]) iterator.next()),
          String.valueOf((Long) iterator.next()));
      }

      return hash;
    }

    public String toString() {
      return "PUBSUB_NUMSUB_MAP<String, String>";
    }

  };

  public static final Builder<Set<String>> STRING_SET = new Builder<Set<String>>() {
    @SuppressWarnings("unchecked")
    public Set<String> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<String> result = new HashSet<String>(l.size());
      for (final byte[] barray : l) {
        if (barray == null) {
          result.add(null);
        } else {
          result.add(SafeEncoder.encode(barray));
        }
      }
      return result;
    }

    public String toString() {
      return "Set<String>";
    }

  };

  public static final Builder<List<byte[]>> BYTE_ARRAY_LIST = new Builder<List<byte[]>>() {
    @SuppressWarnings("unchecked")
    public List<byte[]> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;

      return l;
    }

    public String toString() {
      return "List<byte[]>";
    }
  };

  public static final Builder<Set<byte[]>> BYTE_ARRAY_ZSET = new Builder<Set<byte[]>>() {
    @SuppressWarnings("unchecked")
    public Set<byte[]> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<byte[]> result = new LinkedHashSet<byte[]>(l);
      for (final byte[] barray : l) {
        if (barray == null) {
          result.add(null);
        } else {
          result.add(barray);
        }
      }
      return result;
    }

    public String toString() {
      return "ZSet<byte[]>";
    }
  };
  public static final Builder<Map<byte[], byte[]>> BYTE_ARRAY_MAP = new Builder<Map<byte[], byte[]>>() {
    @SuppressWarnings("unchecked")
    public Map<byte[], byte[]> build(Object data) {
      final List<byte[]> flatHash = (List<byte[]>) data;
      final Map<byte[], byte[]> hash = new JedisByteHashMap();
      final Iterator<byte[]> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        hash.put(iterator.next(), iterator.next());
      }

      return hash;
    }

    public String toString() {
      return "Map<byte[], byte[]>";
    }

  };

  public static final Builder<Set<String>> STRING_ZSET = new Builder<Set<String>>() {
    @SuppressWarnings("unchecked")
    public Set<String> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<String> result = new LinkedHashSet<String>(l.size());
      for (final byte[] barray : l) {
        if (barray == null) {
          result.add(null);
        } else {
          result.add(SafeEncoder.encode(barray));
        }
      }
      return result;
    }

    public String toString() {
      return "ZSet<String>";
    }

  };

  public static final Builder<Set<Tuple>> TUPLE_ZSET = new Builder<Set<Tuple>>() {
    @SuppressWarnings("unchecked")
    public Set<Tuple> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<Tuple> result = new LinkedHashSet<Tuple>(l.size());
      Iterator<byte[]> iterator = l.iterator();
      while (iterator.hasNext()) {
        result.add(new Tuple(SafeEncoder.encode(iterator.next()), Double.valueOf(SafeEncoder
            .encode(iterator.next()))));
      }
      return result;
    }

    public String toString() {
      return "ZSet<Tuple>";
    }

  };

  public static final Builder<Set<Tuple>> TUPLE_ZSET_BINARY = new Builder<Set<Tuple>>() {
    @SuppressWarnings("unchecked")
    public Set<Tuple> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<Tuple> result = new LinkedHashSet<Tuple>(l.size());
      Iterator<byte[]> iterator = l.iterator();
      while (iterator.hasNext()) {
        result.add(new Tuple(iterator.next(), Double.valueOf(SafeEncoder.encode(iterator.next()))));
      }

      return result;

    }

    public String toString() {
      return "ZSet<Tuple>";
    }
  };

  public static final Builder<List<Slowlog>> SLOWLOG_LIST = new Builder<List<Slowlog>>() {
    @Override
    public List<Slowlog> build(Object data) {
      if (null == data) {
        return null;
      }
      return Slowlog.from((List<Object>) data);
    }

    public String toString() {
      return "List<Slowlog>";
    }
  };

  public static final Builder<Object> EVAL_RESULT = new Builder<Object>() {

    @Override
    public Object build(Object data) {
      return evalResult(data);
    }

    public String toString() {
      return "Eval<Object>";
    }

    private Object evalResult(Object result) {
      if (result instanceof byte[]) return SafeEncoder.encode((byte[]) result);

      if (result instanceof List<?>) {
        List<?> list = (List<?>) result;
        List<Object> listResult = new ArrayList<Object>(list.size());
        for (Object bin : list) {
          listResult.add(evalResult(bin));
        }

        return listResult;
      }

      return result;
    }

  };

  public static final Builder<Object> EVAL_BINARY_RESULT = new Builder<Object>() {

    @Override
    public Object build(Object data) {
      return evalResult(data);
    }

    public String toString() {
      return "Eval<Object>";
    }

    private Object evalResult(Object result) {
      if (result instanceof List<?>) {
        List<?> list = (List<?>) result;
        List<Object> listResult = new ArrayList<Object>(list.size());
        for (Object bin : list) {
          listResult.add(evalResult(bin));
        }

        return listResult;
      }

      return result;
    }

  };

  public static final Builder<Object> EVAL_STRING = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      if (data instanceof byte[]) return SafeEncoder.encode((byte[]) data);

      if (data instanceof List<?>) {
        List<?> list = (List<?>) data;
        List<Object> listResult = new ArrayList<Object>(list.size());
        for (Object bin : list) {
          listResult.add(build(bin));
        }

        return listResult;
      }

      return data;
    }

    public String toString() {
      return "EVAL_STRING";
    }

  };

}
