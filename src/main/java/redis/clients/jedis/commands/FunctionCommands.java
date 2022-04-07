package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.resps.FunctionStats;
import redis.clients.jedis.resps.LibraryInfo;

import java.util.List;

public interface FunctionCommands {

  /**
   * Invoke a function.
   * @param name
   * @param keys
   * @param args
   */
  Object fcall(String name, List<String> keys, List<String> args);

  /**
   * This is a read-only variant of the {@link FunctionCommands#fcall(String, List, List) FCALL}
   * command that cannot execute commands that modify data.
   */
  Object fcallReadonly(String name, List<String> keys, List<String> args);

  /**
   * This command deletes the library called library-name and all functions in it.
   * If the library doesn't exist, the server returns an error.
   * @param libraryName
   * @return OK
   */
  String functionDelete(String libraryName);

  /**
   * Return the serialized payload of loaded libraries. You can restore the
   * serialized payload later with the {@link FunctionBinaryCommands#functionRestore(byte[], FunctionRestorePolicy) FUNCTION RESTORE} command.
   * @return the serialized payload
   */
  byte[] functionDump();

  /**
   * Deletes all the libraries, unless called with the optional mode argument, the
   * 'lazyfree-lazy-user-flush' configuration directive sets the effective behavior.
   * @return OK
   */
  String functionFlush();

  /**
   * Deletes all the libraries, unless called with the optional mode argument, the
   * 'lazyfree-lazy-user-flush' configuration directive sets the effective behavior.
   * @param mode ASYNC: Asynchronously flush the libraries, SYNC: Synchronously flush the libraries.
   * @return OK
   */
  String functionFlush(FlushMode mode);

  /**
   * Kill a function that is currently executing. The command can be used only on functions
   * that did not modify the dataset during their execution.
   * @return OK
   */
  String functionKill();

  /**
   * Return information about the functions and libraries.
   * @return {@link LibraryInfo}
   */
  List<LibraryInfo> functionList();

  /**
   * Return information about the functions and libraries.
   * @param libraryNamePattern a pattern for matching library names
   * @return {@link LibraryInfo}
   */
  List<LibraryInfo> functionList(String libraryNamePattern);

  /**
   * Similar to {@link FunctionCommands#functionList() FUNCTION LIST} but include the
   * libraries source implementation in the reply.
   * @see FunctionCommands#functionList()
   * @return {@link LibraryInfo}
   */
  List<LibraryInfo> functionListWithCode();

  /**
   * Similar to {@link FunctionCommands#functionList(String) FUNCTION LIST} but include the
   * libraries source implementation in the reply.
   * @see FunctionCommands#functionList(String)
   * @param libraryNamePattern a pattern for matching library names
   * @return {@link LibraryInfo}
   */
  List<LibraryInfo> functionListWithCode(String libraryNamePattern);

  /**
   * Load a library to Redis.
   * @param functionCode the source code. The library payload must start
   *                     with Shebang statement that provides a metadata
   *                     about the library (like the engine to use and the library name).
   *                     Shebang format: #!<engine name> name=<library name>.
   *                     Currently engine name must be lua.
   * @return The library name that was loaded
   */
  String functionLoad(String functionCode);

  /**
   * Load a library to Redis. Will replace the current library if it already exists.
   * @param functionCode the source code
   * @return The library name that was loaded
   */
  String functionLoadReplace(String functionCode);

  /**
   * Restore libraries from the serialized payload. Default policy is APPEND.
   * @param serializedValue the serialized payload
   * @return OK
   */
  String functionRestore(byte[] serializedValue);

  /**
   * Restore libraries from the serialized payload.
   * @param serializedValue the serialized payload
   * @param policy can be {@link FunctionRestorePolicy FLUSH, APPEND or REPLACE}
   * @return OK
   */
  String functionRestore(byte[] serializedValue, FunctionRestorePolicy policy);

  /**
   * Return information about the function that's currently running and information
   * about the available execution engines.
   * @return {@link FunctionStats}
   */
  FunctionStats functionStats();

}
