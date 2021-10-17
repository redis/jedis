package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.params.FailoverParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.AccessControlUser;
import redis.clients.jedis.resps.LCSMatchResult;

//Legacy
public interface AdvancedBinaryJedisCommands {

  String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      byte[]... keys);

  LCSMatchResult strAlgoLCSStrings(final byte[] strA, final byte[] strB, final StrAlgoLCSParams params);
}
