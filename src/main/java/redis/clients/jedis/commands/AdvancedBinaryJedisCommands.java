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

  List<Object> roleBinary();

  long move(byte[] key, int dbIndex);

  List<byte[]> configGet(byte[] pattern);

  String configSet(byte[] parameter, byte[] value);

  String slowlogReset();

  long slowlogLen();

  List<Object> slowlogGetBinary();

  List<Object> slowlogGetBinary(long entries);

  Long objectRefcount(byte[] key);

  byte[] objectEncoding(byte[] key);

  Long objectIdletime(byte[] key);

  List<byte[]> objectHelpBinary();

  Long objectFreq(byte[] key);

  String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      byte[]... keys);

  String clientKill(byte[] ipPort);

  String clientKill(String ip, int port);

  long clientKill(ClientKillParams params);

  byte[] clientGetnameBinary();

  byte[] clientListBinary();

  byte[] clientListBinary(ClientType type);

  byte[] clientListBinary(long... clientIds);

  byte[] clientInfoBinary();

  String clientSetname(byte[] name);

  long clientId();

  long clientUnblock(long clientId, UnblockType unblockType);

  String clientPause(long timeout);

  String clientPause(long timeout, ClientPauseMode mode);

  byte[] memoryDoctorBinary();

  Long memoryUsage(byte[] key);

  Long memoryUsage(byte[] key, int samples);

  String failover();

  String failover(FailoverParams failoverParams);

  String failoverAbort();

  byte[] aclWhoAmIBinary();

  byte[] aclGenPassBinary();

  List<byte[]> aclListBinary();

  List<byte[]> aclUsersBinary();

  AccessControlUser aclGetUser(byte[] name);

  String aclSetUser(byte[] name);

  String aclSetUser(byte[] name, byte[]... keys);

  long aclDelUser(byte[] name);

  List<byte[]> aclCatBinary();

  List<byte[]> aclCat(byte[] category);

  List<byte[]> aclLogBinary();

  List<byte[]> aclLogBinary(int limit);

  byte[] aclLog(byte[] options);

  String aclLoad();

  String aclSave();

  LCSMatchResult strAlgoLCSStrings(final byte[] strA, final byte[] strB, final StrAlgoLCSParams params);
}
