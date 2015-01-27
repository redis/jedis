package redis.clients.jedis;

import java.util.List;
import java.util.Map;

public interface SentinelCommands {
  public List<Map<String, String>> sentinelMasters();

  public List<String> sentinelGetMasterAddrByName(String masterName);

  public Long sentinelReset(String pattern);

  public List<Map<String, String>> sentinelSlaves(String masterName);

  public String sentinelFailover(String masterName);

  public String sentinelMonitor(String masterName, String ip, int port, int quorum);

  public String sentinelRemove(String masterName);

  public String sentinelSet(String masterName, Map<String, String> parameterMap);
}
