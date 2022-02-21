package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Module;

public interface ModuleCommands {

  String moduleLoad(String path);

  String moduleLoad(String path, String... args);

  String moduleUnload(String name);

  List<Module> moduleList();
}
