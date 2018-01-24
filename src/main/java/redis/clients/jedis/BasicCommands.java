package redis.clients.jedis;

public interface BasicCommands {

  String ping();

  String quit();

  String flushDB();

  Long dbSize();

  String select(int index);

  String swapDB(int index1, int index2);

  String flushAll();

  String auth(String password);

  String save();

  String bgsave();

  String bgrewriteaof();

  Long lastsave();

  String shutdown();

  String info();

  String info(String section);

  String slaveof(String host, int port);

  String slaveofNoOne();

  Long getDB();

  String debug(DebugParams params);

  String configResetStat();

  String configRewrite();

  Long waitReplicas(int replicas, long timeout);
}
