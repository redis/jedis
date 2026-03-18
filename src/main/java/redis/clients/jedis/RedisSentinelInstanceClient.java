package redis.clients.jedis;

import redis.clients.jedis.builders.StandaloneClientBuilder;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.sentinel.api.SentinelInstanceClient;

import java.util.List;
import java.util.Map;

import static redis.clients.jedis.Protocol.Command.SENTINEL;
import static redis.clients.jedis.Protocol.SentinelKeyword.*;

final class RedisSentinelInstanceClient extends UnifiedJedis implements SentinelInstanceClient {

  RedisSentinelInstanceClient(CommandExecutor commandExecutor,
      ConnectionProvider connectionProvider, CommandObjects commandObjects,
      RedisProtocol redisProtocol, Cache cache) {
    super(commandExecutor, connectionProvider, commandObjects, redisProtocol, cache);
  }

  @Override
  public String sentinelMyId() {
    CommandObject<String> cmd = new CommandObject<>(new CommandArguments(SENTINEL).add(MYID),
        BuilderFactory.STRING);
    return executeCommand(cmd);
  }

  @Override
  public List<Map<String, String>> sentinelMasters() {
    CommandObject<List<Map<String, String>>> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(MASTERS), BuilderFactory.STRING_MAP_LIST);
    return executeCommand(cmd);
  }

  @Override
  public Map<String, String> sentinelMaster(String masterName) {
    CommandObject<Map<String, String>> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(MASTER).add(masterName), BuilderFactory.STRING_MAP);
    return executeCommand(cmd);
  }

  @Override
  public List<Map<String, String>> sentinelSentinels(String masterName) {
    CommandObject<List<Map<String, String>>> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(SENTINELS).add(masterName),
        BuilderFactory.STRING_MAP_LIST);
    return executeCommand(cmd);
  }

  @Override
  public List<String> sentinelGetMasterAddrByName(String masterName) {
    CommandObject<List<String>> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(GET_MASTER_ADDR_BY_NAME).add(masterName),
        BuilderFactory.STRING_LIST);
    return executeCommand(cmd);
  }

  @Override
  public Long sentinelReset(String pattern) {
    CommandObject<Long> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(RESET).add(pattern), BuilderFactory.LONG);
    return executeCommand(cmd);
  }

  @Override
  public List<Map<String, String>> sentinelSlaves(String masterName) {
    CommandObject<List<Map<String, String>>> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(SLAVES).add(masterName), BuilderFactory.STRING_MAP_LIST);
    return executeCommand(cmd);
  }

  @Override
  public List<Map<String, String>> sentinelReplicas(String masterName) {
    CommandObject<List<Map<String, String>>> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(REPLICAS).add(masterName),
        BuilderFactory.STRING_MAP_LIST);
    return executeCommand(cmd);
  }

  @Override
  public String sentinelFailover(String masterName) {
    CommandObject<String> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(FAILOVER).add(masterName), BuilderFactory.STRING);
    return executeCommand(cmd);
  }

  @Override
  public String sentinelMonitor(String masterName, String ip, int port, int quorum) {
    CommandObject<String> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(MONITOR).add(masterName).add(ip).add(port).add(quorum),
        BuilderFactory.STRING);
    return executeCommand(cmd);
  }

  @Override
  public String sentinelRemove(String masterName) {
    CommandObject<String> cmd = new CommandObject<>(
        new CommandArguments(SENTINEL).add(REMOVE).add(masterName), BuilderFactory.STRING);
    return executeCommand(cmd);
  }

  @Override
  public String sentinelSet(String masterName, Map<String, String> parameterMap) {
    CommandArguments args = new CommandArguments(SENTINEL).add(SET).add(masterName);
    parameterMap.forEach((key, value) -> args.add(key).add(value));
    CommandObject<String> cmd = new CommandObject<>(args, BuilderFactory.STRING);
    return executeCommand(cmd);
  }

  static class Builder extends StandaloneClientBuilder<SentinelInstanceClient> {

    protected Builder self() {
      return this;
    }

    @Override
    protected SentinelInstanceClient createClient() {
      return new RedisSentinelInstanceClient(commandExecutor, connectionProvider, commandObjects,
          clientConfig.getRedisProtocol(), cache);
    }
  }

  /**
   * Create a new builder for configuring RedisSentinelInstanceClient instances.
   * @return a new {@link RedisSentinelInstanceClient.Builder} instance
   */
  static Builder builder() {
    return new Builder();
  }
}
