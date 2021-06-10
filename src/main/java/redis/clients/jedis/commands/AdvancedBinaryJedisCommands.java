package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.AccessControlUser;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.ClientKillParams;

public interface AdvancedBinaryJedisCommands {

  long move(byte[] key, int dbIndex);

  List<byte[]> configGet(byte[] pattern);

  String configSet(byte[] parameter, byte[] value);

  /**
   * @deprecated Use {@link #configSet(byte[], byte[])}.
   */
  @Deprecated
  default String configSetBinary(byte[] parameter, byte[] value) {
    return configSet(parameter, value);
  }

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

  byte[] clientListBinary(long... clientIds);

  byte[] clientInfoBinary();

  String clientSetname(byte[] name);

  long clientId();

  long clientUnblock(long clientId, UnblockType unblockType);

  byte[] memoryDoctorBinary();

  Long memoryUsage(byte[] key);

  Long memoryUsage(byte[] key, int samples);

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
}
