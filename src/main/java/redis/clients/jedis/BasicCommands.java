package redis.clients.jedis;

public interface BasicCommands {

    String ping();

    String quit();

    String flushDB();

    Long dbSize();

    String select(int index);

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

    Long waitReplicas(int replicas, long timeout);
}
