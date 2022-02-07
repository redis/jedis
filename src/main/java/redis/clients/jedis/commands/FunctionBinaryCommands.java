package redis.clients.jedis.commands;

import redis.clients.jedis.args.RestorePolicy;
import redis.clients.jedis.params.FunctionLoadParams;
import redis.clients.jedis.resps.LibraryInfo;

import java.util.List;

public interface FunctionBinaryCommands {

    byte[] fcall(byte[] name, List<byte[]> keys, List<byte[]> args);

    byte[] fcallReadonly(byte[] name, List<byte[]> keys, List<byte[]> args);

    /**
     * This command deletes the library called library-name and all functions in it.
     * If the library doesn't exist, the server returns an error.
     * @param libraryName
     * @return OK
     */
    String functionDelete(byte[] libraryName);

    /**
     * Return the serialized payload of loaded libraries. You can restore the
     * serialized payload later with the {@link FunctionBinaryCommands#functionRestore(byte[], RestorePolicy) FUNCTION RESTORE} command.
     * @return the serialized payload
     */
    byte[] functionDump();

    /**
     * Return information about the functions and libraries.
     * @param libraryNamePattern a pattern for matching library names
     * @return {@link LibraryInfo}
     */
    List<LibraryInfo> functionList(byte[] libraryNamePattern);

    /**
     * Similar to {@link FunctionBinaryCommands#functionList(byte[]) FUNCTION LIST} but include the
     * libraries source implementation in the reply.
     * @see FunctionBinaryCommands#functionList(byte[])
     * @param libraryNamePattern a pattern for matching library names
     * @return {@link LibraryInfo}
     */
    List<LibraryInfo> functionListWithCode(byte[] libraryNamePattern);

    /**
     * Load a library to Redis.
     * @param engineName the name of the execution engine for the library
     * @param libraryName the unique name of the library
     * @param functionCode the source code
     * @return OK
     */
    String functionLoad(byte[] engineName, byte[] libraryName, byte[] functionCode);

    /**
     * Load a library to Redis.
     * @param engineName the name of the execution engine for the library
     * @param libraryName the unique name of the library
     * @param params {@link FunctionLoadParams}
     * @param functionCode the source code
     * @return OK
     */
    String functionLoad(byte[] engineName, byte[] libraryName, FunctionLoadParams params, byte[] functionCode);

    /**
     * Restore libraries from the serialized payload. Default policy is APPEND.
     * @param serializedValue the serialized payload
     * @return OK
     */
    String functionRestore(byte[] serializedValue);

    /**
     * Restore libraries from the serialized payload.
     * @param serializedValue the serialized payload
     * @param policy can be {@link RestorePolicy FLUSH, APPEND or REPLACE}
     * @return OK
     */
    String functionRestore(byte[] serializedValue, RestorePolicy policy);

//    functionStat
    
}
