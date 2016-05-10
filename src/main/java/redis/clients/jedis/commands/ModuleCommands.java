package redis.clients.jedis.commands;

import redis.clients.jedis.Module;

import java.util.List;

public interface ModuleCommands {
  String moduleLoad(String path);
  String moduleUnload(String name);
  List<Module> moduleList();
}
