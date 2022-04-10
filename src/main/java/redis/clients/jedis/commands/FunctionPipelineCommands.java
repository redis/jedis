package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.resps.FunctionStats;
import redis.clients.jedis.resps.LibraryInfo;

import java.util.List;

public interface FunctionPipelineCommands {
  
  Response<Object> fcall(String name, List<String> keys, List<String> args);
  
  Response<Object> fcallReadonly(String name, List<String> keys, List<String> args);
  
  Response<String> functionDelete(String libraryName);

  Response<byte[]> functionDump();

  Response<String> functionFlush();
  
  Response<String> functionFlush(FlushMode mode);
  
  Response<String> functionKill();

  Response<List<LibraryInfo>> functionList();

  Response<List<LibraryInfo>> functionList(String libraryNamePattern);

  Response<List<LibraryInfo>> functionListWithCode();

  Response<List<LibraryInfo>> functionListWithCode(String libraryNamePattern);

  Response<String> functionLoad(String functionCode);

  Response<String> functionLoadReplace(String functionCode);

  Response<String> functionRestore(byte[] serializedValue);

  Response<String> functionRestore(byte[] serializedValue, FunctionRestorePolicy policy);

  Response<FunctionStats> functionStats();

}
