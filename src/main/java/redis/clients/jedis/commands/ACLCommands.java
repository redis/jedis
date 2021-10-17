package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.resps.AccessControlLogEntry;
import redis.clients.jedis.resps.AccessControlUser;

public interface ACLCommands {

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

}
