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

  long move(String key, int dbIndex);

  List<String> configGet(String pattern);

  String configSet(String parameter, String value);

  String slowlogReset();

  long slowlogLen();

  List<Slowlog> slowlogGet();

  List<Slowlog> slowlogGet(long entries);

  Long objectRefcount(String key);

  String objectEncoding(String key);

  Long objectIdletime(String key);

  List<String> objectHelp();

  Long objectFreq(String key);

  String migrate(String host, int port, String key, int destinationDB, int timeout);

  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      String... keys);

  String clientKill(String ipPort);

  String clientKill(String ip, int port);

  long clientKill(ClientKillParams params);

  String clientGetname();

  String clientList();

  String clientList(ClientType type);

  String clientList(long... clientIds);

  String clientInfo();

  String clientSetname(String name);

  long clientId();

  long clientUnblock(long clientId, UnblockType unblockType);

  String clientPause(long timeout);

  String clientPause(long timeout, ClientPauseMode mode);

  String memoryDoctor();

  Long memoryUsage(String key);

  Long memoryUsage(String key, int samples);

  String failover();

  String failover(FailoverParams failoverParams);

  String failoverAbort();

  String aclWhoAmI();

  String aclGenPass();

  List<String> aclList();

  List<String> aclUsers();

  AccessControlUser aclGetUser(String name);

  String aclSetUser(String name);

  String aclSetUser(String name, String... keys);

  long aclDelUser(String name);

  List<String> aclCat();

  List<String> aclCat(String category);

  List<AccessControlLogEntry> aclLog();

  List<AccessControlLogEntry> aclLog(int limit);

  String aclLog(String options);

  String aclLoad();

  String aclSave();

  LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params);
}
