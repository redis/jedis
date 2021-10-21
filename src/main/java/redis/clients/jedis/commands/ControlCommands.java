package redis.clients.jedis.commands;

import java.util.List;

public interface ControlCommands extends ACLCommands, ClientCommands {

  List<Object> role();

  Long objectRefcount(String key);

  String objectEncoding(String key);

  Long objectIdletime(String key);

  List<String> objectHelp();

  Long objectFreq(String key);

  String memoryDoctor();

  Long memoryUsage(String key);

  Long memoryUsage(String key, int samples);

}
