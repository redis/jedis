package redis.clients.jedis.gears.resps;

import redis.clients.jedis.Builder;
import redis.clients.jedis.util.KeyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static redis.clients.jedis.BuilderFactory.*;
import static redis.clients.jedis.gears.resps.FunctionStreamInfo.STREAM_INFO_LIST;

@Deprecated
public class StreamTriggerInfo {
  private final String name;
  private final String description;
  private final String prefix;
  private final boolean trim;
  private final long window;
  private final List<FunctionStreamInfo> streams;

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getPrefix() {
    return prefix;
  }
  public boolean isTrim() {
    return trim;
  }

  public long getWindow() {
    return window;
  }

  public List<FunctionStreamInfo> getStreams() {
    return streams;
  }

  public StreamTriggerInfo(String name, String description, String prefix,
    long window, boolean trim, List<FunctionStreamInfo> streams) {
    this.name = name;
    this.description = description;
    this.prefix = prefix;
    this.window = window;
    this.trim = trim;
    this.streams = streams;
  }
  public StreamTriggerInfo(String name) {
    this(name, null, null, 0, false, Collections.emptyList());
  }

  public StreamTriggerInfo(String name, String description, String prefix,
    long window, boolean trim) {
    this(name, description, prefix, window, trim, Collections.emptyList());
  }

  @Deprecated
  public static final Builder<List<StreamTriggerInfo>> STREAM_TRIGGER_INFO_LIST = new Builder<List<StreamTriggerInfo>>() {
    @Override
    public List<StreamTriggerInfo> build(Object data) {
      List<Object> dataAsList = (List<Object>) data;
      if (!dataAsList.isEmpty()) {
        boolean isListOfList = dataAsList.get(0).getClass().isAssignableFrom(ArrayList.class);
        if (isListOfList) {
          if (((List<List<Object>>)data).get(0).get(0) instanceof KeyValue) {
            List<List<KeyValue>> dataAsKeyValues = (List<List<KeyValue>>)data;
            return dataAsKeyValues.stream().map(keyValues -> {
              String name = null;
              String description = null;
              String prefix = null;
              long window = 0;
              boolean trim = false;
              List<FunctionStreamInfo> streams = null;

              for (KeyValue kv : keyValues) {
                switch (STRING.build(kv.getKey())) {
                  case "name":
                    name = STRING.build(kv.getValue());
                    break;
                  case "description":
                    description = STRING.build(kv.getValue());
                    break;
                  case "prefix":
                    prefix = STRING.build(kv.getValue());
                    break;
                  case "window":
                    window = LONG.build(kv.getValue());
                    break;
                  case "trim":
                    trim = BOOLEAN.build(kv.getValue());
                    break;
                  case "streams":
                    streams = STREAM_INFO_LIST.build(kv.getValue());
                    break;
                }
              }
              return new StreamTriggerInfo(name, description, prefix, window, trim, streams);
            }).collect(Collectors.toList());
          } else {
            return dataAsList.stream().map((pairObject) -> (List<Object>) pairObject).map((pairList) -> {
                StreamTriggerInfo result = null;
                switch (pairList.size()) {
                  case 1:
                    result = new StreamTriggerInfo(STRING.build(pairList.get(0)));
                    break;
                  case 10:
                    result = new StreamTriggerInfo( //
                      STRING.build(pairList.get(3)),          // name
                      STRING.build(pairList.get(1)),          // description
                      STRING.build(pairList.get(5)),          // prefix
                      LONG.build(pairList.get(9)),            // window
                      BOOLEAN.build(pairList.get(7))          // trim
                    );
                    break;
                  case 12:
                    result = new StreamTriggerInfo( //
                      STRING.build(pairList.get(3)),          // name
                      STRING.build(pairList.get(1)),          // description
                      STRING.build(pairList.get(5)),          // prefix
                      LONG.build(pairList.get(11)),           // window
                      BOOLEAN.build(pairList.get(9)),         // trim
                      STREAM_INFO_LIST.build(pairList.get(7)) // streams
                    );
                    break;
                }
                return result;
              }) //
              .collect(Collectors.toList());
          }
        } else {
          return dataAsList.stream() //
            .map(STRING::build).map((name) -> new StreamTriggerInfo(name, null, null, 0, false)) //
            .collect(Collectors.toList());
        }
      } else {
        return Collections.emptyList();
      }
    }
  };
}
