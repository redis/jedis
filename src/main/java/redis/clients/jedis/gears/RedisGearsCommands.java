package redis.clients.jedis.gears;

import redis.clients.jedis.gears.resps.GearsLibraryInfo;

import java.util.List;

public interface RedisGearsCommands {
  String tFunctionLoad(String libraryCode);
  String tFunctionLoad(String libraryCode, TFunctionLoadParams params);
  List<GearsLibraryInfo> tFunctionList(TFunctionListParams params);
  List<GearsLibraryInfo> tFunctionList();
  String tFunctionDelete(String libraryName);
  Object tFunctionCall(String library, String function, List<String> keys, List<String> args);
  Object tFunctionCallAsync(String library, String function, List<String> keys, List<String> args);
}
