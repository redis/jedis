package redis.clients.jedis.resps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.KeyValue;

public class FunctionStats {

  private final Map<String, Object> runningScript;
  private final Map<String, Map<String, Object>> engines;

  public FunctionStats(Map<String, Object> script, Map<String, Map<String, Object>> engines) {
    this.runningScript = script;
    this.engines = engines;
  }

  public Map<String, Object> getRunningScript() {
    return runningScript;
  }

  public Map<String, Map<String, Object>> getEngines() {
    return engines;
  }

  public static final Builder<FunctionStats> FUNCTION_STATS_BUILDER = new Builder<FunctionStats>() {

    @Override
    public FunctionStats build(Object data) {
      if (data == null) return null;
      List list = (List) data;
      if (list.isEmpty()) return null;

      if (list.get(0) instanceof KeyValue) {

        Map<String, Object> runningScriptMap = null;
        Map<String, Map<String, Object>> enginesMap = null;

        for (KeyValue kv : (List<KeyValue>) list) {
          switch (BuilderFactory.STRING.build(kv.getKey())) {
            case "running_script":
              runningScriptMap = BuilderFactory.ENCODED_OBJECT_MAP.build(kv.getValue());
              break;
            case "engines":
              List<KeyValue> ilist = (List<KeyValue>) kv.getValue();
              enginesMap = new LinkedHashMap<>(ilist.size());
              for (KeyValue ikv : (List<KeyValue>) kv.getValue()) {
                enginesMap.put(BuilderFactory.STRING.build(ikv.getKey()),
                    BuilderFactory.ENCODED_OBJECT_MAP.build(ikv.getValue()));
              }
              break;
          }
        }

        return new FunctionStats(runningScriptMap, enginesMap);
      }

      Map<String, Object> runningScriptMap = list.get(1) == null ? null
          : BuilderFactory.ENCODED_OBJECT_MAP.build(list.get(1));

      List<Object> enginesList = (List<Object>) list.get(3);

      Map<String, Map<String, Object>> enginesMap = new LinkedHashMap<>(enginesList.size() / 2);
      for (int i = 0; i < enginesList.size(); i += 2) {
        enginesMap.put(BuilderFactory.STRING.build(enginesList.get(i)),
            BuilderFactory.ENCODED_OBJECT_MAP.build(enginesList.get(i + 1)));
      }

      return new FunctionStats(runningScriptMap, enginesMap);
    }
  };

}
