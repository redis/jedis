package redis.server.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-client state for Redis connections. Tracks: - Client ID (unique identifier) - Client name
 * (optional, set via CLIENT SETNAME) - Current database (0-15, default 0) - Authentication status -
 * Library info (name, version) - Protocol version (RESP3 only for initial implementation) - Push
 * messages queue (for notifications)
 * <p>
 * <b>Protocol Support</b>: This implementation only supports RESP3 protocol. Protocol version is
 * fixed at 3 and cannot be changed.
 */
public class ClientState {

  private static final AtomicLong CLIENT_ID_GENERATOR = new AtomicLong(1);

  // Client identification
  private final long id;
  private String name;

  // Database selection
  private int database = 0;

  // Authentication
  private boolean authenticated = false;

  // Library information
  private String libName;
  private String libVersion;

  // Protocol version (RESP3 only - fixed at 3 for initial implementation)
  private final int protocolVersion = 3;

  // Client tracking (for CLIENT TRACKING ON/OFF)
  private boolean trackingEnabled = false;

  // Push messages for RESP3
  private final BlockingQueue<String> pushMessages = new LinkedBlockingQueue<>();

  // Generic info map for extensibility
  private final Map<String, String> info = new HashMap<>();

  public ClientState() {
    this.id = CLIENT_ID_GENERATOR.getAndIncrement();
  }

  // ===== Getters and Setters =====

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getDatabase() {
    return database;
  }

  public void setDatabase(int database) {
    if (database < 0 || database > 15) {
      throw new IllegalArgumentException("Database must be between 0 and 15");
    }
    this.database = database;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }

  public String getLibName() {
    return libName;
  }

  public void setLibName(String libName) {
    this.libName = libName;
  }

  public String getLibVersion() {
    return libVersion;
  }

  public void setLibVersion(String libVersion) {
    this.libVersion = libVersion;
  }

  /**
   * Get the protocol version. Always returns 3 (RESP3).
   * @return protocol version (always 3)
   */
  public int getProtocolVersion() {
    return protocolVersion;
  }

  /**
   * Set protocol version. This implementation only supports RESP3.
   * @param protocolVersion must be 3
   * @throws IllegalArgumentException if protocolVersion is not 3
   */
  public void setProtocolVersion(int protocolVersion) {
    if (protocolVersion != 3) {
      throw new IllegalArgumentException(
          "Only RESP3 (protocol version 3) is supported in this implementation");
    }
    // No-op since protocolVersion is final and already 3
  }

  public BlockingQueue<String> getPushMessages() {
    return pushMessages;
  }

  public Map<String, String> getInfo() {
    return info;
  }

  public boolean isTrackingEnabled() {
    return trackingEnabled;
  }

  public void setTrackingEnabled(boolean trackingEnabled) {
    this.trackingEnabled = trackingEnabled;
  }

  @Override
  public String toString() {
    return "ClientState{" + "id=" + id + ", name='" + name + '\'' + ", database=" + database
        + ", authenticated=" + authenticated + ", libName='" + libName + '\'' + ", libVersion='"
        + libVersion + '\'' + ", protocolVersion=" + protocolVersion + ", trackingEnabled="
        + trackingEnabled + '}';
  }
}
