package redis.clients.jedis;

import redis.clients.jedis.params.stream.*;
import redis.clients.util.JedisByteHashMap;
import redis.clients.util.SafeEncoder;

import java.util.*;

public final class BuilderFactory {
  public static final Builder<Double> DOUBLE = new Builder<Double>() {
    @Override
    public Double build(Object data) {
      String string = STRING.build(data);
      if (string == null) return null;
      try {
        return Double.valueOf(string);
      } catch (NumberFormatException e) {
        if (string.equals("inf") || string.equals("+inf")) return Double.POSITIVE_INFINITY;
        if (string.equals("-inf")) return Double.NEGATIVE_INFINITY;
        throw e;
      }
    }

    @Override
    public String toString() {
      return "double";
    }
  };
  public static final Builder<Boolean> BOOLEAN = new Builder<Boolean>() {
    @Override
    public Boolean build(Object data) {
      return ((Long) data) == 1;
    }

    @Override
    public String toString() {
      return "boolean";
    }
  };
  public static final Builder<byte[]> BYTE_ARRAY = new Builder<byte[]>() {
    @Override
    public byte[] build(Object data) {
      return ((byte[]) data); // deleted == 1
    }

    @Override
    public String toString() {
      return "byte[]";
    }
  };

  public static final Builder<Long> LONG = new Builder<Long>() {
    @Override
    public Long build(Object data) {
      return (Long) data;
    }

    @Override
    public String toString() {
      return "long";
    }

  };
  public static final Builder<String> STRING = new Builder<String>() {
    @Override
    public String build(Object data) {
      return data == null ? null : SafeEncoder.encode((byte[]) data);
    }

    @Override
    public String toString() {
      return "string";
    }

  };
  public static final Builder<List<String>> STRING_LIST = new Builder<List<String>>() {
    @Override
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

    @Override
    public String toString() {
      return "List<String>";
    }

  };
  public static final Builder<Map<String, String>> STRING_MAP = new Builder<Map<String, String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> build(Object data) {
      final List<byte[]> flatHash = (List<byte[]>) data;
      final Map<String, String> hash = new HashMap<String, String>(flatHash.size()/2, 1);
      final Iterator<byte[]> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        hash.put(SafeEncoder.encode(iterator.next()), SafeEncoder.encode(iterator.next()));
      }

      return hash;
    }

    @Override
    public String toString() {
      return "Map<String, String>";
    }

  };

  public static final Builder<Map<String, String>> PUBSUB_NUMSUB_MAP = new Builder<Map<String, String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> build(Object data) {
      final List<Object> flatHash = (List<Object>) data;
      final Map<String, String> hash = new HashMap<String, String>(flatHash.size()/2, 1);
      final Iterator<Object> iterator = flatHash.iterator();
      while (iterator.hasNext()) {
        hash.put(SafeEncoder.encode((byte[]) iterator.next()),
          String.valueOf((Long) iterator.next()));
      }

      return hash;
    }

    @Override
    public String toString() {
      return "PUBSUB_NUMSUB_MAP<String, String>";
    }

  };

  public static final Builder<Set<String>> STRING_SET = new Builder<Set<String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<String> result = new HashSet<String>(l.size(), 1);
      for (final byte[] barray : l) {
        if (barray == null) {
          result.add(null);
        } else {
          result.add(SafeEncoder.encode(barray));
        }
      }
      return result;
    }

    @Override
    public String toString() {
      return "Set<String>";
    }

  };

  public static final Builder<List<byte[]>> BYTE_ARRAY_LIST = new Builder<List<byte[]>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<byte[]> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;

      return l;
    }

    @Override
    public String toString() {
      return "List<byte[]>";
    }
  };

  public static final Builder<Set<byte[]>> BYTE_ARRAY_ZSET = new Builder<Set<byte[]>>() {
    @Override
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

    @Override
    public String toString() {
      return "ZSet<byte[]>";
    }
  };
  public static final Builder<Map<byte[], byte[]>> BYTE_ARRAY_MAP = new Builder<Map<byte[], byte[]>>() {
    @Override
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

    @Override
    public String toString() {
      return "Map<byte[], byte[]>";
    }

  };

  public static final Builder<Set<String>> STRING_ZSET = new Builder<Set<String>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<String> result = new LinkedHashSet<String>(l.size(), 1);
      for (final byte[] barray : l) {
        if (barray == null) {
          result.add(null);
        } else {
          result.add(SafeEncoder.encode(barray));
        }
      }
      return result;
    }

    @Override
    public String toString() {
      return "ZSet<String>";
    }

  };

  public static final Builder<Set<Tuple>> TUPLE_ZSET = new Builder<Set<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<Tuple> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<Tuple> result = new LinkedHashSet<Tuple>(l.size()/2, 1);
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

  public static final Builder<Set<Tuple>> TUPLE_ZSET_BINARY = new Builder<Set<Tuple>>() {
    @Override
    @SuppressWarnings("unchecked")
    public Set<Tuple> build(Object data) {
      if (null == data) {
        return null;
      }
      List<byte[]> l = (List<byte[]>) data;
      final Set<Tuple> result = new LinkedHashSet<Tuple>(l.size()/2, 1);
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

  public static final Builder<Object> EVAL_RESULT = new Builder<Object>() {

    @Override
    public Object build(Object data) {
      return evalResult(data);
    }

    @Override
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

    @Override
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
      List<GeoCoordinate> responseCoordinate = new ArrayList<GeoCoordinate>(responses.size());
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

      List<GeoRadiusResponse> responses = new ArrayList<GeoRadiusResponse>(objectList.size());
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

  private BuilderFactory() {
    throw new InstantiationError( "Must not instantiate this class" );
  }

  public static final Builder<StreamParams> STREAM_PARAMS = new Builder<StreamParams>() {
    @Override
    @SuppressWarnings("unchecked")
    public StreamParams build(Object data) {
      List<Object> element = (List<Object>) data;
      StreamParams streamParams = new StreamParams();
      streamParams.setEntryId(STRING.build(element.get(0)));
      Map<String,String> map = STRING_MAP.build(element.get(1));
      for(Map.Entry<String,String> entry:map.entrySet()){
        streamParams.addPair(entry.getKey(),entry.getValue());
      }
      return streamParams;
    }

    @Override
    public String toString(){
      return "StreamParams";
    }
  };

  public static final Builder<StreamInfo> STREAM_INFO = new Builder<StreamInfo>() {
    @Override
    @SuppressWarnings("unchecked")
    public StreamInfo build(Object data) {
      List<Object> info = (List<Object>) data;
      StreamInfo infoParams = new StreamInfo();
      infoParams.setLength(LONG.build(info.get(1)));
      infoParams.setRadixTreeKeys(LONG.build(info.get(3)));
      infoParams.setRadixTreeNodes(LONG.build(info.get(5)));
      infoParams.setGroups(LONG.build(info.get(7)));
      infoParams.setLastGeneratedId(STRING.build(info.get(9)));
      infoParams.setFirstEntry(STREAM_PARAMS.build(info.get(11)));
      infoParams.setLastEntry(STREAM_PARAMS.build(info.get(13)));
      return infoParams;
    }

    public String toString(){
      return "StreamInfo";
    }
  };

  public static final Builder<GroupInfo> GROUP_INFO = new Builder<GroupInfo>() {
    @Override
    @SuppressWarnings("unchecked")
    public GroupInfo build(Object data) {
      List<Object> list = (List<Object>) data;
      GroupInfo groupInfo = new GroupInfo();
      groupInfo.setName(STRING.build(list.get(1)));
      groupInfo.setConsumers(LONG.build(list.get(3)));
      groupInfo.setPending(LONG.build(list.get(5)));
      groupInfo.setLastDeliveredId(STRING.build(list.get(7)));
      return groupInfo;
    }

    public String toString(){
      return "GroupInfo";
    }
  };

  public static final Builder<ConsumerInfo> CONSUMER_INFO = new Builder<ConsumerInfo>() {
    @Override
    @SuppressWarnings("unchecked")
    public ConsumerInfo build(Object data) {
      List<Object> list = (List<Object>) data;
      ConsumerInfo consumerInfo = new ConsumerInfo();
      consumerInfo.setName(STRING.build(list.get(1)));
      consumerInfo.setPending(LONG.build(list.get(3)));
      if(list.size()>4){
        consumerInfo.setIdle(LONG.build(list.get(5)));
      }
      return consumerInfo;
    }

    public String toString(){
      return "ConsumerInfo";
    }
  };

  public static final Builder<GroupPendingInfo> GROUP_PENDING_INFO = new Builder<GroupPendingInfo>() {
    @Override
    @SuppressWarnings("unchecked")
    public GroupPendingInfo build(Object data) {
      List<Object> list = (List<Object>) data;
      GroupPendingInfo groupPendingInfo=new GroupPendingInfo();
      groupPendingInfo.setCount(LONG.build(list.get(0)));
      groupPendingInfo.setOldestEntryId(STRING.build(list.get(1)));
      groupPendingInfo.setNewestEntryId(STRING.build(list.get(2)));
      List<Object> consumers = (List<Object>) list.get(3);
      List<ConsumerInfo> consumerInfos = new ArrayList<ConsumerInfo>();
      ConsumerInfo consumerInfo;
      for(Object consumer:consumers){
        List<Object> consumerInfoList = (List<Object>) consumer;
        consumerInfo=new ConsumerInfo();
        consumerInfo.setName(STRING.build(consumerInfoList.get(0)));
        consumerInfo.setPending(LONG.build(consumerInfoList.get(1)));
        consumerInfos.add(consumerInfo);
      }
      groupPendingInfo.setConsumers(consumerInfos);
      return groupPendingInfo;
    }

    public String toString(){
      return "GroupPendingInfo";
    }
  };

  public static final Builder<PendingInfo> PENDING_INFO = new Builder<PendingInfo>() {
    @Override
    @SuppressWarnings("unchecked")
    public PendingInfo build(Object data) {
      List<Object> list = (List<Object>) data;
      PendingInfo pendingInfo=new PendingInfo();
      pendingInfo.setEntryId(STRING.build(list.get(0)));
      pendingInfo.setConsumer(STRING.build(list.get(1)));
      pendingInfo.setIdle(LONG.build(list.get(2)));
      pendingInfo.setDeliveredTimes(LONG.build(list.get(3)));
      return pendingInfo;
    }

    public String toString(){
      return "PendingInfo";
    }
  };

}
