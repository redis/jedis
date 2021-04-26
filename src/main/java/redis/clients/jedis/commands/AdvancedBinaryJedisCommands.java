package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.AccessControlUser;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.ClientKillParams;

public interface AdvancedBinaryJedisCommands {

  List<byte[]> configGet(byte[] pattern);

  /**
   * @param parameter
   * @param value
   * @return OK
   * @deprecated The return type will be changed to {@link String}, representing {@code OK} response,
   * in next major release. If you are not checking you continue using this method. Otherwise, you
   * can choose to use either {@link #configSet(byte[], byte[]) this method} or
   * {@link #configSetBinary(byte[], byte[])}.
   */
  @Deprecated
  byte[] configSet(byte[] parameter, byte[] value);

  String configSetBinary(byte[] parameter, byte[] value);

  String slowlogReset();

  Long slowlogLen();

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

  Long clientKill(ClientKillParams params);

  Long clientUnblock(long clientId, UnblockType unblockType);

  byte[] clientGetnameBinary();

  byte[] clientListBinary();

  byte[] clientListBinary(long... clientIds);

  byte[] clientInfoBinary();

  String clientSetname(byte[] name);

  Long clientId();

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

  Long aclDelUser(byte[] name);

  List<byte[]> aclCatBinary();

  List<byte[]> aclCat(byte[] category);

  List<byte[]> aclLogBinary();

  List<byte[]> aclLogBinary(int limit);

  byte[] aclLog(byte[] options);

  String aclLoad();

  String aclSave();
}
