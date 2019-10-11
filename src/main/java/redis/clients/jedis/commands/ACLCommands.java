package redis.clients.jedis.commands;

import redis.clients.jedis.UserACL;

import java.util.List;

public interface ACLCommands {

  String aclWhoAmI();

  List<String> aclList();

  String aclSetUser(String name);

  String aclSetUser(String name, String... keys);

  Long aclDelUser(String name);

  UserACL aclGetUser(String name);

}
