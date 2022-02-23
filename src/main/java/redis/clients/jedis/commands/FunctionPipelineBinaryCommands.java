package redis.clients.jedis.commands;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.params.FunctionLoadParams;
import redis.clients.jedis.resps.LibraryInfo;

import java.util.List;

public interface FunctionPipelineBinaryCommands {

  Response<Object> fcall(byte[] name, List<byte[]> keys, List<byte[]> args);

  Response<Object> fcallReadonly(byte[] name, List<byte[]> keys, List<byte[]> args);

  Response<String> functionDelete(byte[] libraryName);

  Response<byte[]> functionDump();

  Response<List<LibraryInfo>> functionList(byte[] libraryNamePattern);

  Response<List<LibraryInfo>> functionListWithCode(byte[] libraryNamePattern);

  Response<String> functionLoad(byte[] engineName, byte[] libraryName, byte[] functionCode);

  Response<String> functionLoad(byte[] engineName, byte[] libraryName, FunctionLoadParams params, byte[] functionCode);

  Response<String> functionRestore(byte[] serializedValue);

  Response<String> functionRestore(byte[] serializedValue, FunctionRestorePolicy policy);
    
}
