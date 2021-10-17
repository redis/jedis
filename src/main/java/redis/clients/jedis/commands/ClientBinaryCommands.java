package redis.clients.jedis.commands;

import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.args.UnblockType;
import redis.clients.jedis.params.ClientKillParams;

public interface ClientBinaryCommands {

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

}
