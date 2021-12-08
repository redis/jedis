package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

//Legacy
public interface SentinelCommands {

  String sentinelMyId();

  List<Map<String, String>> sentinelMasters();

  Map<String, String> sentinelMaster(String masterName);

  List<Map<String, String>> sentinelSentinels(String masterName);

  List<String> sentinelGetMasterAddrByName(String masterName);

  Long sentinelReset(String pattern);

  /**
   * @deprecated Use {@link SentinelCommands#sentinelReplicas(java.lang.String)}.
   */
  @Deprecated
  List<Map<String, String>> sentinelSlaves(String masterName);

  List<Map<String, String>> sentinelReplicas(String masterName);

  String sentinelFailover(String masterName);

  String sentinelMonitor(String masterName, String ip, int port, int quorum);

  String sentinelRemove(String masterName);

  String sentinelSet(String masterName, Map<String, String> parameterMap);
}
