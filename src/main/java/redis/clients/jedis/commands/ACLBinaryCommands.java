package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.resps.AccessControlUser;

public interface ACLBinaryCommands {

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
