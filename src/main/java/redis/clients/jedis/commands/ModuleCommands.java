package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Module;
import redis.clients.jedis.params.ModuleLoadExParams;

public interface ModuleCommands {

  /**
   * Load and initialize the Redis module from the dynamic library specified by the path argument.
   *
   * @param path should be the absolute path of the library, including the full filename
   * @return OK
   */
  String moduleLoad(String path);

  /**
   * Load and initialize the Redis module from the dynamic library specified by the path argument.
   *
   * @param path should be the absolute path of the library, including the full filename
   * @param args additional arguments are passed unmodified to the module
   * @return OK
   */
  String moduleLoad(String path, String... args);

  /**
   * Loads a module from a dynamic library at runtime with configuration directives.
   * <p>
   * This is an extended version of the MODULE LOAD command.
   * <p>
   * It loads and initializes the Redis module from the dynamic library specified by the path
   * argument. The path should be the absolute path of the library, including the full filename.
   * <p>
   * You can use the optional CONFIG argument to provide the module with configuration directives.
   * Any additional arguments that follow the ARGS keyword are passed unmodified to the module.
   *
   * @param path should be the absolute path of the library, including the full filename
   * @param params as in description
   * @return OK
   */
  String moduleLoadEx(String path, ModuleLoadExParams params);

  /**
   * Unload the module specified by name. Note that the module's name is reported by the
   * {@link ModuleCommands#moduleList() MODULE LIST} command, and may differ from the dynamic
   * library's filename.
   *
   * @param name
   * @return OK
   */
  String moduleUnload(String name);

  /**
   * Return information about the modules loaded to the server.
   *
   * @return list of {@link Module}
   */
  List<Module> moduleList();
}
