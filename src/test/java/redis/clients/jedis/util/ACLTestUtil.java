package redis.clients.jedis.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import redis.clients.jedis.resps.AccessControlLogEntry;

/**
 * Utility class for ACL testing operations.
 */
public final class ACLTestUtil {

  private ACLTestUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Filters a list of ACL log entries by client ID.
   * @param entries the list of ACL log entries to filter
   * @param clientId the client ID to filter by
   * @return a new list containing only entries matching the specified client ID
   */
  public static List<AccessControlLogEntry> filterByClientId(List<AccessControlLogEntry> entries,
      long clientId) {
    String clientIdStr = String.valueOf(clientId);
    return entries.stream().filter(entry -> {
      Map<String, String> clientInfo = entry.getClientInfo();
      return clientInfo != null && clientIdStr.equals(clientInfo.get("id"));
    }).collect(Collectors.toList());
  }
}
