package redis.clients.jedis.commands;

import java.util.List;

public interface ScriptingKeyCommands {

  /**
   * <b><a href="http://redis.io/commands/eval">Eval Command</a></b>
   * Use to evaluate scripts using the Lua interpreter built into Redis starting from version 2.6.0.
   * @param script Lua 5.1 script. The script does not need to define a Lua function (and should not).
   *              It is just a Lua program that will run in the context of the Redis server.
   * @return The result of the evaluated script
   */
  Object eval(String script);

  /**
   * <b><a href="http://redis.io/commands/eval">Eval Command</a></b>
   * Use to evaluate scripts using the Lua interpreter built into Redis starting from version 2.6.0.
   * @param script Lua 5.1 script. The script does not need to define a Lua function (and should not).
   *              It is just a Lua program that will run in the context of the Redis server.
   * @param keyCount the count of the provided keys
   * @param params arguments that can be accessed from the script
   * @return The result of the evaluated script
   */
  Object eval(String script, int keyCount, String... params);

  /**
   * <b><a href="http://redis.io/commands/eval">Eval Command</a></b>
   * Use to evaluate scripts using the Lua interpreter built into Redis starting from version 2.6.0.
   * @param script Lua 5.1 script. The script does not need to define a Lua function (and should not).
   *              It is just a Lua program that will run in the context of the Redis server.
   * @param keys arguments that can be accessed by the script
   * @param args additional arguments should not represent key names and can be accessed by the script
   * @return The result of the evaluated script
   */
  Object eval(String script, List<String> keys, List<String> args);

  /**
   * Readonly version of {@link ScriptingKeyCommands#eval(String, List, List) EVAL}
   * @see ScriptingKeyCommands#eval(String, List, List)
   * @param script Lua 5.1 script. The script does not need to define a Lua function (and should not).
   *              It is just a Lua program that will run in the context of the Redis server.
   * @param keys arguments that can be accessed by the script
   * @param args additional arguments should not represent key names and can be accessed by the script
   * @return The result of the evaluated script
   */
  Object evalReadonly(String script, List<String> keys, List<String> args);

  /**
   * <b><a href="http://redis.io/commands/evalsha">EvalSha Command</a></b>
   * Similar to {@link ScriptingKeyCommands#eval(String) EVAL}, but the script cached on the server
   * side by its SHA1 digest. Scripts are cached on the server side using the SCRIPT LOAD command.
   * @see ScriptingKeyCommands#eval(String)
   * @param sha1 the script
   * @return The result of the evaluated script
   */
  Object evalsha(String sha1);

  /**
   * <b><a href="http://redis.io/commands/evalsha">EvalSha Command</a></b>
   * Similar to {@link ScriptingKeyCommands#eval(String, int, String...)}  EVAL}, but the script cached on the server
   * side by its SHA1 digest. Scripts are cached on the server side using the SCRIPT LOAD command.
   * @see ScriptingKeyCommands#eval(String, int, String...)
   * @param sha1 the script
   * @return The result of the evaluated script
   */
  Object evalsha(String sha1, int keyCount, String... params);

  /**
   * <b><a href="http://redis.io/commands/evalsha">EvalSha Command</a></b>
   * Similar to {@link ScriptingKeyCommands#eval(String, List, List)}  EVAL}, but the script cached on the server
   * side by its SHA1 digest. Scripts are cached on the server side using the SCRIPT LOAD command.
   * @see ScriptingKeyCommands#eval(String, List, List)
   * @param sha1 the script
   * @return The result of the evaluated script
   */
  Object evalsha(String sha1, List<String> keys, List<String> args);

  /**
   * Readonly version of {@link ScriptingKeyCommands#evalsha(String, List, List) EVAL}
   * @see ScriptingKeyCommands#evalsha(String, List, List)
   * @param sha1 the script
   * @return The result of the evaluated script
   */
  Object evalshaReadonly(String sha1, List<String> keys, List<String> args);
}
