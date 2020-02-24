package redis.clients.jedis.commands;

import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.util.Slowlog;

import java.util.List;

public interface AdvancedClusterCommands {

  String objectEncoding(String key);

}
