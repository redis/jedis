package redis.clients.jedis;

import java.util.List;

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

  String migrate(String host, int port, String key, int destinationDB, int timeout);

  String clientKill(String ipPort);

  String clientKill(String ip, int port);

  String clientGetname();

  String clientList();

  String clientSetname(String name);
}
