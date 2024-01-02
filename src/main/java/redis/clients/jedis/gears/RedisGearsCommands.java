package redis.clients.jedis.gears;

import redis.clients.jedis.gears.resps.GearsLibraryInfo;

import java.util.List;

public interface RedisGearsCommands {

  default String tFunctionLoad(String libraryCode) {
    return tFunctionLoad(libraryCode, TFunctionLoadParams.loadParams());
  }

  String tFunctionLoad(String libraryCode, TFunctionLoadParams params);

  default List<GearsLibraryInfo> tFunctionList() {
    return tFunctionList(TFunctionListParams.listParams());
  }

  List<GearsLibraryInfo> tFunctionList(TFunctionListParams params);

  String tFunctionDelete(String libraryName);

  Object tFunctionCall(String library, String function, List<String> keys, List<String> args);

  Object tFunctionCallAsync(String library, String function, List<String> keys, List<String> args);
}
