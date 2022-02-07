package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.params.FunctionLoadParams;
import redis.clients.jedis.resps.LibraryInfo;

import java.util.List;

public interface FunctionCommands {

    byte[] fcall(String name, List<String> keys, List<String> args);

    byte[] fcallReadonly(String name, List<String> keys, List<String> args);

    /**
     * This command deletes the library called library-name and all functions in it.
     * If the library doesn't exist, the server returns an error.
     * @param libraryName
     * @return OK
     */
    String functionDelete(String libraryName);
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
     * @param engineName the name of the execution engine for the library
     * @param libraryName the unique name of the library
     * @param functionCode the source code
     * @return OK
     */
    String functionLoad(String engineName, String libraryName, String functionCode);

    /**
     * Load a library to Redis.
     * @param engineName the name of the execution engine for the library
     * @param libraryName the unique name of the library
     * @param params {@link FunctionLoadParams}
     * @param functionCode the source code
     * @return OK
     */
    String functionLoad(String engineName, String libraryName, FunctionLoadParams params, String functionCode);

//    functionStat

}
