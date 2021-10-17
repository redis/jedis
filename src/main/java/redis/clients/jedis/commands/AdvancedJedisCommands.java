package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.params.FailoverParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.AccessControlLogEntry;
import redis.clients.jedis.resps.AccessControlUser;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.resps.Slowlog;

//Legacy
public interface AdvancedJedisCommands {

  List<Object> role();

  String migrate(String host, int port, String key, int destinationDB, int timeout);

  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      String... keys);

  LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params);
}
