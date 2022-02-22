package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Module;

public interface ModuleCommands {

  /**
   * Load and initialize the Redis module from the dynamic library specified by the path argument.
   * @param path should be the absolute path of the library, including the full filename
   * @return OK
   */
  String moduleLoad(String path);

  /**
   * Load and initialize the Redis module from the dynamic library specified by the path argument.
   * @param path should be the absolute path of the library, including the full filename
   * @param args additional arguments are passed unmodified to the module
   * @return OK
   */
  String moduleLoad(String path, String... args);

  /**
   * Unload the module specified by name. Note that the module's name is reported by the
   * {@link ModuleCommands#moduleList() MODULE LIST} command, and may differ from the dynamic library's filename.
   * @param name
   * @return OK
   */
  String moduleUnload(String name);

  /**
   * Return information about the modules loaded to the server.
   * @return list of {@link Module}
   */
  List<Module> moduleList();
}
