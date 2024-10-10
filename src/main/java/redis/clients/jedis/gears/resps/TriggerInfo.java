package redis.clients.jedis.gears.resps;

import redis.clients.jedis.Builder;
import redis.clients.jedis.util.KeyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static redis.clients.jedis.BuilderFactory.LONG;
import static redis.clients.jedis.BuilderFactory.STRING;

@Deprecated
public class TriggerInfo {
  private final String name;
  private final String description;

  private final String lastError;

  private final long lastExecutionTime;

  private final long numFailed;

  private final long numFinished;

  private final long numSuccess;

  private final long numTrigger;

  private final long totalExecutionTime;



  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getLastError() {
    return lastError;
  }

  public long getLastExecutionTime() {
    return lastExecutionTime;
  }

  public long getNumFailed() {
    return numFailed;
  }

  public long getNumFinished() {
    return numFinished;
  }

  public long getNumSuccess() {
    return numSuccess;
  }

  public long getNumTrigger() {
    return numTrigger;
  }

  public long getTotalExecutionTime() {
    return totalExecutionTime;
  }

  public TriggerInfo(String name, String description, String lastError, long numFinished, long numSuccess,
    long numFailed, long numTrigger, long lastExecutionTime, long totalExecutionTime) {
    this.name = name;
    this.description = description;
    this.lastError = lastError;
    this.numFinished = numFinished;
    this.numSuccess = numSuccess;
    this.numFailed = numFailed;
    this.numTrigger = numTrigger;
    this.lastExecutionTime = lastExecutionTime;
    this.totalExecutionTime = totalExecutionTime;
  }

  @Deprecated
  public static final Builder<List<TriggerInfo>> KEYSPACE_TRIGGER_INFO_LIST = new Builder<List<TriggerInfo>>() {
    @Override
    public List<TriggerInfo> build(Object data) {
      List<Object> dataAsList = (List<Object>) data;
      if (!dataAsList.isEmpty()) {
        boolean isListOfList = dataAsList.get(0).getClass().isAssignableFrom(ArrayList.class);
        if (isListOfList) {
          if (((List<List<Object>>)data).get(0).get(0) instanceof KeyValue) {
            List<List<KeyValue>> dataAsKeyValues = (List<List<KeyValue>>)data;
            return dataAsKeyValues.stream().map(keyValues -> {
              String name = null;
              String description = null;
              String lastError = null;
              long lastExecutionTime = 0;
              long numFailed = 0;
              long numFinished = 0;
              long numSuccess = 0;
              long numTrigger = 0;
              long totalExecutionTime = 0;

              for (KeyValue kv : keyValues) {
                switch (STRING.build(kv.getKey())) {
                  case "name":
                    name = STRING.build(kv.getValue());
                    break;
                  case "description":
                    description = STRING.build(kv.getValue());
                    break;
                  case "last_error":
                    lastError = STRING.build(kv.getValue());
                    break;
                  case "last_execution_time":
                    lastExecutionTime = LONG.build(kv.getValue());
                    break;
                  case "num_failed":
                    numFailed = LONG.build(kv.getValue());
                    break;
                  case "num_finished":
                    numFinished = LONG.build(kv.getValue());
                    break;
                  case "num_success":
                    numSuccess = LONG.build(kv.getValue());
                    break;
                  case "num_trigger":
                    numTrigger = LONG.build(kv.getValue());
                    break;
                  case "total_execution_time":
                    totalExecutionTime = LONG.build(kv.getValue());
                    break;
                }
              }
              return new TriggerInfo(name, description, lastError, numFinished, numSuccess, numFailed, numTrigger,
                lastExecutionTime, totalExecutionTime);
            }).collect(Collectors.toList());
          } else {
            return dataAsList.stream().map((pairObject) -> (List<Object>) pairObject)
              .map((pairList) -> new TriggerInfo(STRING.build(pairList.get(7)),   // name
                STRING.build(pairList.get(1)),   // description
                STRING.build(pairList.get(3)),   // last_error
                LONG.build(pairList.get(11)),    // num_finished
                LONG.build(pairList.get(13)),    // num_success
                LONG.build(pairList.get(9)),     // num_failed
                LONG.build(pairList.get(15)),    // num_trigger
                LONG.build(pairList.get(5)),     // last_execution_time
                LONG.build(pairList.get(17))     // total_execution_time
              ))//
              .collect(Collectors.toList());
          }
        } else {
          return dataAsList.stream() //
            .map(STRING::build)//
            .map((name) -> new TriggerInfo(name, null, null, 0,0,0,0,0,0)) //
            .collect(Collectors.toList());
        }
      } else {
        return Collections.emptyList();
      }
    }
  };
}
