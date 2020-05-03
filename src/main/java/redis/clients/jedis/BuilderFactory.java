package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.SafeEncoder;

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
      final ArrayList<String> result = new ArrayList<>(l.size());
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
      final Map<String, String> hash = new HashMap<>(flatHash.size()/2, 1);
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
      final Map<String, String> hash = new HashMap<>(flatHash.size()/2, 1);
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
      final Set<String> result = new HashSet<>(l.size(), 1);
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
      final Set<byte[]> result = new LinkedHashSet<>(l);
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
      final Set<String> result = new LinkedHashSet<>(l.size(), 1);
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
      final Set<Tuple> result = new LinkedHashSet<>(l.size()/2, 1);
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
        List<Object> listResult = new ArrayList<>(list.size());
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
        List<Object> listResult = new ArrayList<>(list.size());
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

      for (List<Object> moduleResp: objectList) {
        Module m = new Module(SafeEncoder.encode((byte[]) moduleResp.get(1)), ((Long) moduleResp.get(3)).intValue());
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
   * Create a AccessControlUser object from the ACL GETUSER < > result
   */
  public static final Builder<AccessControlUser> ACCESS_CONTROL_USER = new Builder<AccessControlUser>() {
    @Override
    public AccessControlUser build(Object data) {
      if (data == null) {
        return null;
      }

      List<List<Object>> objectList = (List<List<Object>>) data;
      if (objectList.isEmpty()) { return null; }

      AccessControlUser accessControlUser = new AccessControlUser();

      // flags
      List<Object> flags = objectList.get(1);
      for (Object f : flags) {
        accessControlUser.addFlag(SafeEncoder.encode((byte[]) f));
      }

      // passwords
      List<Object> passwords = objectList.get(3);
      for (Object p : passwords) {
        accessControlUser.addPassword(SafeEncoder.encode((byte[]) p));
      }

      // commands
      accessControlUser.setCommands(SafeEncoder.encode((byte[]) (Object) objectList.get(5)));

      // keys
      List<Object> keys = objectList.get(7);
      for (Object k : keys) {
        accessControlUser.addKey(SafeEncoder.encode((byte[]) k));
      }

      return accessControlUser;
    }

    @Override
    public String toString() {
      return "AccessControlUser";
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

  public static final Builder<StreamEntryID> STREAM_ENTRY_ID = new Builder<StreamEntryID>() {
    @Override
    @SuppressWarnings("unchecked")
    public  StreamEntryID build(Object data) {
      if (null == data) {
        return null;
      }
      String id = SafeEncoder.encode((byte[])data);
      return new StreamEntryID(id);
    }

    @Override
    public String toString() {
      return "StreamEntryID";
    }
  };
  

  public static final Builder<List<StreamEntry>> STREAM_ENTRY_LIST = new Builder<List<StreamEntry>>() {
    @Override
    @SuppressWarnings("unchecked")
    public  List<StreamEntry> build(Object data) {
      if (null == data) {
        return null;
      }
      List<ArrayList<Object>> objectList = (List<ArrayList<Object>>) data;

      List<StreamEntry> responses = new ArrayList<>(objectList.size()/2);
      if (objectList.isEmpty()) {
        return responses;
      }

      for(ArrayList<Object> res : objectList) {
        String entryIdString = SafeEncoder.encode((byte[])res.get(0));
        StreamEntryID entryID = new StreamEntryID(entryIdString);
        List<byte[]> hash = (List<byte[]>)res.get(1);
        
        Iterator<byte[]> hashIterator = hash.iterator();
        Map<String, String> map = new HashMap<>(hash.size()/2);
        while(hashIterator.hasNext()) {
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

  public static final Builder<StreamEntry> STREAM_ENTRY = new Builder<StreamEntry>() {
    @Override
    @SuppressWarnings("unchecked")
    public  StreamEntry build(Object data) {
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
      Map<String, String> map = new HashMap<>(hash.size() / 2);
      while (hashIterator.hasNext()) {
        map.put(SafeEncoder.encode(hashIterator.next()),
            SafeEncoder.encode(hashIterator.next()));
      }
      StreamEntry streamEntry = new StreamEntry(entryID, map);


      return streamEntry;
    }

    @Override
    public String toString() {
      return "StreamEntry";
    }
  };
  
  public static final Builder<List<StreamPendingEntry>> STREAM_PENDING_ENTRY_LIST = new Builder<List<StreamPendingEntry>>() {
    @Override
    @SuppressWarnings("unchecked")
    public  List<StreamPendingEntry> build(Object data) {
      if (null == data) {
        return null;
      }
      
      List<Object> streamsEntries = (List<Object>)data;
      List<StreamPendingEntry> result = new ArrayList<>(streamsEntries.size());
      for(Object streamObj : streamsEntries) {
        List<Object> stream = (List<Object>)streamObj;
        String id = SafeEncoder.encode((byte[])stream.get(0));
        String consumerName = SafeEncoder.encode((byte[])stream.get(1));
        long idleTime = BuilderFactory.LONG.build(stream.get(2));      
        long deliveredTimes = BuilderFactory.LONG.build(stream.get(3));
        result.add(new StreamPendingEntry(new StreamEntryID(id), consumerName, idleTime, deliveredTimes));
      }
      return result;
    }

    @Override
    public String toString() {
      return "List<StreamPendingEntry>";
    }
  };

  public static final Builder<StreamInfo> STREAM_INFO = new Builder<StreamInfo>() {


    Map<String,Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String,Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamInfo.LAST_GENERATED_ID,STREAM_ENTRY_ID);
      tempMappingFunctions.put(StreamInfo.FIRST_ENTRY,STREAM_ENTRY);
      tempMappingFunctions.put(StreamInfo.LENGTH, LONG);
      tempMappingFunctions.put(StreamInfo.RADIX_TREE_KEYS, LONG);
      tempMappingFunctions.put(StreamInfo.RADIX_TREE_NODES, LONG);
      tempMappingFunctions.put(StreamInfo.LAST_ENTRY,STREAM_ENTRY);
      tempMappingFunctions.put(StreamInfo.GROUPS, LONG);

      return  tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamInfo build(Object data) {
      if (null == data) {
        return null;
      }

      List<Object> streamsEntries = (List<Object>)data;
      Iterator<Object> iterator = streamsEntries.iterator();

      StreamInfo streamInfo = new StreamInfo(
          createMapFromDecodingFunctions(iterator,mappingFunctions));
      return streamInfo;
    }

    @Override
    public String toString() {
      return "StreamInfo";
    }
  };

  public static final Builder<List<StreamGroupInfo>> STREAM_GROUP_INFO_LIST = new Builder<List<StreamGroupInfo>>() {

    Map<String,Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {

      Map<String,Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamGroupInfo.NAME,STRING);
      tempMappingFunctions.put(StreamGroupInfo.CONSUMERS, LONG);
      tempMappingFunctions.put(StreamGroupInfo.PENDING, LONG);
      tempMappingFunctions.put(StreamGroupInfo.LAST_DELIVERED,STREAM_ENTRY_ID);

      return  tempMappingFunctions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public  List<StreamGroupInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamGroupInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>)data;
      Iterator<Object> groupsArray = streamsEntries.iterator();

      while (groupsArray.hasNext()) {

        List<Object> groupInfo = (List<Object>) groupsArray.next();

        Iterator<Object> groupInfoIterator = groupInfo.iterator();

        StreamGroupInfo streamGroupInfo = new StreamGroupInfo(
            createMapFromDecodingFunctions(groupInfoIterator,mappingFunctions));
        list.add(streamGroupInfo);

      }
      return list;

    }

    @Override
    public String toString() {
      return "List<StreamGroupInfo>";
    }
  };

  public static final Builder<List<StreamConsumersInfo>> STREAM_CONSUMERS_INFO_LIST = new Builder<List<StreamConsumersInfo>>() {

    Map<String,Builder> mappingFunctions = createDecoderMap();

    private Map<String, Builder> createDecoderMap() {
      Map<String,Builder> tempMappingFunctions = new HashMap<>();
      tempMappingFunctions.put(StreamConsumersInfo.NAME,STRING);
      tempMappingFunctions.put(StreamConsumersInfo.IDLE, LONG);
      tempMappingFunctions.put(StreamGroupInfo.PENDING, LONG);
      tempMappingFunctions.put(StreamGroupInfo.LAST_DELIVERED,STRING);
      return tempMappingFunctions;

    }

    @Override
    @SuppressWarnings("unchecked")
    public  List<StreamConsumersInfo> build(Object data) {
      if (null == data) {
        return null;
      }

      List<StreamConsumersInfo> list = new ArrayList<>();
      List<Object> streamsEntries = (List<Object>)data;
      Iterator<Object> groupsArray = streamsEntries.iterator();

      while (groupsArray.hasNext()) {

        List<Object> groupInfo = (List<Object>) groupsArray.next();

        Iterator<Object> consumerInfoIterator = groupInfo.iterator();

        StreamConsumersInfo streamGroupInfo = new StreamConsumersInfo(
            createMapFromDecodingFunctions(consumerInfoIterator,mappingFunctions));
        list.add(streamGroupInfo);

      }
      return list;

    }

    @Override
    public String toString() {
      return "List<StreamConsumersInfo>";
    }
  };

  public static final Builder<Object> OBJECT = new Builder<Object>() {
    @Override
    public Object build(Object data) {
      return data;
    }
    @Override
    public String toString() {
      return "Object";
    }
  };



  private BuilderFactory() {
    throw new InstantiationError( "Must not instantiate this class" );
  }

  private static Map<String,Object> createMapFromDecodingFunctions( Iterator<Object> iterator, Map<String,Builder> mappingFunctions) {

    Map<String,Object> resultMap = new HashMap<>();
    while (iterator.hasNext()) {

      String mapKey = STRING.build(iterator.next());
      if (mappingFunctions.containsKey(mapKey)) {
        resultMap.put(mapKey, mappingFunctions.get(mapKey).build(iterator.next()));
      } else {   //For future - if we don't find an element in our builder map
        Object unknownData = iterator.next();
        for (Builder b:mappingFunctions.values()) {
          try {
            resultMap.put(mapKey,b.build(unknownData));
            break;
          } catch (ClassCastException e) {
            //We continue with next builder

          }
        }
      }
    }
    return resultMap;
  }

}
