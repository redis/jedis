package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.params.ClientKillParams;
import redis.clients.util.Slowlog;

public interface AdvancedJedisCommands {
  List<String> configGet(String pattern);

  String configSet(String parameter, String value);

  String slowlogReset();

  Long slowlogLen();

  List<Slowlog> slowlogGet();

  List<Slowlog> slowlogGet(long entries);

  Long objectRefcount(String key);

  String objectEncoding(String key);

  Long objectIdletime(String key);

  String clientKill(String ipPort);

  String clientKill(String ip, int port);

  Long clientKill(ClientKillParams params);

  String clientGetname();

  String clientList();

  String clientSetname(String name);
}
