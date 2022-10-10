package redis.clients.jedis.resps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;

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
      List<Object> superMapList = (List<Object>) data;

      Map<String, Object> runningScriptMap = superMapList.get(1) == null ? null
          : BuilderFactory.ENCODED_OBJECT_MAP.build(superMapList.get(1));

      List<Object> enginesList = (List<Object>) superMapList.get(3);

      Map<String, Map<String, Object>> enginesMap = new LinkedHashMap<>(enginesList.size() / 2);
      for (int i = 0; i < enginesList.size(); i += 2) {
        enginesMap.put(BuilderFactory.STRING.build(enginesList.get(i)),
            BuilderFactory.ENCODED_OBJECT_MAP.build(enginesList.get(i + 1)));
      }

      return new FunctionStats(runningScriptMap, enginesMap);
    }
  };

}
