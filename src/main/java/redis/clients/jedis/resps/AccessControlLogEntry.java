package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.*;

/**
 * This class holds information about an Access Control Log entry (returned by ACL LOG command) They
 * can be access via getters. For future purpose there is also {@link #getlogEntry} method that
 * returns a generic {@code Map} - in case where more info is returned from a server
 */
public class AccessControlLogEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String COUNT = "count";
  public static final String REASON = "reason";
  public static final String CONTEXT = "context";
  public static final String OBJECT = "object";
  public static final String USERNAME = "username";
  public static final String AGE_SECONDS = "age-seconds";
  public static final String CLIENT_INFO = "client-info";

  private long count;
  private final String reason;
  private final String context;
  private final String object;
  private final String username;
  private final String ageSeconds;
  private final Map<String, String> clientInfo;
  private final Map<String, Object> logEntry;

  public AccessControlLogEntry(Map<String, Object> map) {
    count = (long) map.get(COUNT);
    reason = (String) map.get(REASON);
    context = (String) map.get(CONTEXT);
    object = (String) map.get(OBJECT);
    username = (String) map.get(USERNAME);
    ageSeconds = (String) map.get(AGE_SECONDS);
    clientInfo = getMapFromRawClientInfo((String) map.get(CLIENT_INFO));
    logEntry = map;
  }

  public long getCount() {
    return count;
  }

  public String getReason() {
    return reason;
  }

  public String getContext() {
    return context;
  }

  public String getObject() {
    return object;
  }

  public String getUsername() {
    return username;
  }

  public String getAgeSeconds() {
    return ageSeconds;
  }

  public Map<String, String> getClientInfo() {
    return clientInfo;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getlogEntry() {
    return logEntry;
  }

  /**
   * Convert the client-info string into a Map of String. When the value is empty, the value in the
   * map is set to an empty string The key order is maintained to reflect the string return by Redis
   * @param clientInfo
   * @return A Map with all client info
   */
  private Map<String, String> getMapFromRawClientInfo(String clientInfo) {
    String[] entries = clientInfo.split(" ");
    Map<String, String> clientInfoMap = new LinkedHashMap<>(entries.length);
    for (String entry : entries) {
      String[] kvArray = entry.split("=");
      clientInfoMap.put(kvArray[0], (kvArray.length == 2) ? kvArray[1] : "");
    }
    return clientInfoMap;
  }

  @Override
  public String toString() {
    return "AccessControlLogEntry{" + "count=" + count + ", reason='" + reason + '\''
        + ", context='" + context + '\'' + ", object='" + object + '\'' + ", username='" + username
        + '\'' + ", ageSeconds='" + ageSeconds + '\'' + ", clientInfo=" + clientInfo + '}';
  }
}
