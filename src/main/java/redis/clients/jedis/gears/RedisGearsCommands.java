package redis.clients.jedis.gears;

import redis.clients.jedis.gears.resps.GearsLibraryInfo;

import java.util.List;

@Deprecated
public interface RedisGearsCommands {

  @Deprecated default String tFunctionLoad(String libraryCode) {
    return tFunctionLoad(libraryCode, TFunctionLoadParams.loadParams());
  }

  @Deprecated String tFunctionLoad(String libraryCode, TFunctionLoadParams params);

  @Deprecated default List<GearsLibraryInfo> tFunctionList() {
    return tFunctionList(TFunctionListParams.listParams());
  }

  @Deprecated List<GearsLibraryInfo> tFunctionList(TFunctionListParams params);

  @Deprecated String tFunctionDelete(String libraryName);

  @Deprecated Object tFunctionCall(String library, String function, List<String> keys, List<String> args);

  @Deprecated Object tFunctionCallAsync(String library, String function, List<String> keys, List<String> args);
}
