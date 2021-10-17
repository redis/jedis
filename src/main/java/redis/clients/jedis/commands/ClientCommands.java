package redis.clients.jedis.commands;

import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.ClientKillParams;

public interface ClientCommands {

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

}
