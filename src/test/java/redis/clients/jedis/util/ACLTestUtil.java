package redis.clients.jedis.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import redis.clients.jedis.BuilderFactory;
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

  /**
   * Filters a list of binary ACL log entries by client ID.
   * <p>
   * This method converts the binary ACL log entries to AccessControlLogEntry objects, filters them
   * by client ID, and returns the filtered binary entries.
   * @param binaryEntries the list of binary ACL log entries to filter (raw Redis response)
   * @param clientId the client ID to filter by
   * @return a new list containing only binary entries matching the specified client ID
   */
  public static List<byte[]> filterBinaryByClientId(List<byte[]> binaryEntries, long clientId) {
    if (binaryEntries == null || binaryEntries.isEmpty()) {
      return new ArrayList<>();
    }

    // Build the structured entries from binary data
    List<AccessControlLogEntry> entries = BuilderFactory.ACCESS_CONTROL_LOG_ENTRY_LIST
        .build(binaryEntries);
    if (entries == null || entries.isEmpty()) {
      return new ArrayList<>();
    }

    // Filter by client ID
    String clientIdStr = String.valueOf(clientId);
    List<Integer> matchingIndices = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      AccessControlLogEntry entry = entries.get(i);
      Map<String, String> clientInfo = entry.getClientInfo();
      if (clientInfo != null && clientIdStr.equals(clientInfo.get("id"))) {
        matchingIndices.add(i);
      }
    }

    // Return the corresponding binary entries
    List<byte[]> result = new ArrayList<>();
    for (Integer index : matchingIndices) {
      result.add(binaryEntries.get(index));
    }
    return result;
  }
}
